package fish.focus.uvms.plugins.inmarsat;

import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.uvms.commons.les.inmarsat.InmarsatDefinition;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatSocketException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.jms.Queue;
import javax.jms.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Startup
@Singleton
public class InmarsatMessageRetriever {

    private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH).put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
    private static final int PATTERN_LENGTH = HEADER_PATTERN.length;

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatMessageRetriever.class);
    private static final String INMARSAT_MESSAGES = "jms/queue/UVMSInmarsatMessages";

    @Resource(mappedName = "java:/" + INMARSAT_MESSAGES)
    private Queue inmarsatMessages;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    private static final String PLUGIN_PROPERTIES = "plugin.properties";
    private static final String SETTINGS_PROPERTIES = "settings.properties";
    private static final String CAPABILITIES_PROPERTIES = "capabilities.properties";
    private ConcurrentMap<String, String> capabilities = null;
    private Properties twoStageApplicationProperties;
    private boolean isEnabled = false;

    @Inject
    private HelperFunctions functions;

    @Inject
    private SettingsHandler settingsHandler;

    private void initialize() {
        isEnabled = false;
        capabilities = new ConcurrentHashMap<>();
        twoStageApplicationProperties = functions.getPropertiesFromFile(this.getClass(), PLUGIN_PROPERTIES);
        Properties twostageProperties = functions.getPropertiesFromFile(this.getClass(), SETTINGS_PROPERTIES);
        Properties twostageCapabilities = functions.getPropertiesFromFile(this.getClass(), CAPABILITIES_PROPERTIES);
        functions.mapToMapFromProperties(settingsHandler.getSettings(), twostageProperties, getRegisterClassName());
        functions.mapToMapFromProperties(capabilities, twostageCapabilities, null);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Settings updated in plugin {}", getRegisterClassName());
            for (Map.Entry<String, String> entry : settingsHandler.getSettings().entrySet()) {
                LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
            }
        }
    }

    @PostConstruct
    private void startup() {
        initialize();
        LOGGER.info("Inmarsat retriever plugin created");
    }

    @PreDestroy
    private void shutdown() {
        LOGGER.info("Inmarsat retriever plugin destroyed");
    }

    //    @Schedule(second = "15", minute = "*", hour = "*", persistent = false)
    @Schedule(minute = "*/3", hour = "*", persistent = false)
    private void retrieveMessages() {
        LOGGER.info("HEARTBEAT retrieveMessages running. IsEnabled=" + isEnabled + " threadId=" + Thread.currentThread().toString());
        Socket socket = null;
        PrintStream output = null;
        try {
            if (isEnabled()) {
                List<String> dnids = getDnids();
                List<String> oceanRegions = getOceanRegions();
                String url = settingsHandler.getSetting("URL", getRegisterClassName());
                String port = settingsHandler.getSetting("PORT", getRegisterClassName());
                String user = settingsHandler.getSetting("USERNAME", getRegisterClassName());
                String pwd = settingsHandler.getSetting("PSW", getRegisterClassName());

                socket = new Socket();
                socket.connect(new InetSocketAddress(url, Integer.parseInt(port)), Constants.SOCKET_TIMEOUT);
                socket.setSoTimeout(Constants.SOCKET_TIMEOUT);

                // logon
                BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                output = new PrintStream(socket.getOutputStream());
                functions.readUntil("name:", input);
                functions.write(user, output);
                functions.readUntil("word:", input);
                functions.sendPwd(output, pwd);
                functions.readUntil(">", input);

                for (String dnid : dnids) {
                    String status = "NOK";
                    LOGGER.info("Trying to download for :{}", dnid);
                    for (String  oceanRegion : oceanRegions) {
                        try {
                            String cmd = "DNID " + dnid + " " + oceanRegion;
                            LOGGER.info(cmd);
                            functions.write(cmd, output);
                            try {
                                byte[] bos = readUntil(">", input);
                                if (bos != null && bos.length > 0) {

                                    String messageControl = new String(bos);
                                    int pos = messageControl.indexOf("T&T");
                                    if(pos < 0){
                                        LOGGER.info(messageControl);
                                        status = "OK";
                                        continue;
                                    }
                                    if (post(bos)) {
                                        status = "OK";
                                    } else {
                                        status = "NOK could not post to queue";
                                    }
                                }
                            } catch (InmarsatSocketException tex) {
                                LOGGER.info("Possible reason : Vessel probably not in that Ocean Region " + String.valueOf(oceanRegion), tex);
                            }
                        } catch (NullPointerException ex) {
                            LOGGER.error("Error when communicating with Telnet", ex);
                            status = "NOK Error when communicating with Telnet";
                        }
                    }
                    LOGGER.info(status);
                }
            }
        } catch (Throwable t) {
            LOGGER.error(t.toString(), t);
        } finally {
            if (output != null) {
                output.print("QUIT \r\n");
                output.flush();
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // OK
                }
            }
        }
    }

    private boolean post(byte[] msg) {
        if (connectionFactory == null) {
            LOGGER.error("No factory. Cannot send messages to queue");
            return false;
        }
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(inmarsatMessages)
        ) {
            BytesMessage message = session.createBytesMessage();
            message.setStringProperty("messagesource", "INMARSAT_C");
            message.writeBytes(msg);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
            String msgId = message.getJMSMessageID();
            LOGGER.info("Added to internal queue " + msgId);
            return true;
        } catch (JMSException e) {
            LOGGER.error(e.toString(), e);
            return false;
        }
    }

    private byte[] readUntil(String pattern, BufferedInputStream in) throws InmarsatSocketException {
        StringBuilder sb = new StringBuilder();
        byte[] contents = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            while ((bytesRead = in.read(contents)) > 0) {
                bos.write(contents, 0, bytesRead);
                String s = new String(contents, 0, bytesRead);
                sb.append(s);
                String currentString = sb.toString().trim();
                if (currentString.endsWith(pattern)) {
                    bos.flush();
                    return bos.toByteArray();
                } else {
                    functions.containsFault(currentString);
                }
            }
            bos.flush();
            return new byte[0];

        } catch (IOException ioe) {
            LOGGER.info(ioe.toString(), ioe);
            return new byte[0];
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getPluginApplicationProperty("application.responseTopicName");
    }

    public String getRegisterClassName() {
        return getPluginApplicationProperty("application.groupid") + "." + getPluginApplicationProperty("application.name");
    }

    private String getPluginApplicationProperty(String key) {
        try {
            return (String) twoStageApplicationProperties.get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * @return list of DNIDs configured
     */
    private List<String> getDnids() {
        String dnidsSettingValue = settingsHandler.getSetting("DNIDS", getRegisterClassName());
        if (StringUtils.isBlank(dnidsSettingValue)) {
            return new ArrayList<>();
        }
        return Arrays.asList(dnidsSettingValue.trim().split(","));
    }

    private List<String> getOceanRegions() {
        String value = settingsHandler.getSetting("OCEAN_REGIONS", getRegisterClassName());
        if((value == null) || (value.trim().length() < 1)){
            return new ArrayList<>();
        }
        List<String> ret = new ArrayList<>();
        String values[] = value.trim().split(",");
        for(String val : values){
            switch( val.trim()){
                case "AOR-W" : ret.add("0");break;
                case "AOR-E" : ret.add("1");break;
                case "POR" : ret.add("2");break;
                case "IOR" : ret.add("3");break;
            }
        }
        return ret;
    }

    public String getApplicationName() {
        try {
            return (String) twoStageApplicationProperties.get("application.name");
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: application.name", getRegisterClassName());
            return null;
        }
    }

    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

    /**
     * Start the twostage. Use this to enable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType start() {
        LOGGER.info(getRegisterClassName() + ".start()");
        try {
            setEnabled(Boolean.TRUE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setEnabled(Boolean.FALSE);
            LOGGER.error("Failed to start {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }

    /**
     * Start the twostage. Use this to disable functionality in the twostage
     *
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType stop() {
        LOGGER.info(getRegisterClassName() + ".stop()");
        try {
            setEnabled(Boolean.FALSE);
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            setEnabled(Boolean.TRUE);
            LOGGER.error("Failed to stop {}", getRegisterClassName());
            return AcknowledgeTypeType.NOK;
        }
    }
}
