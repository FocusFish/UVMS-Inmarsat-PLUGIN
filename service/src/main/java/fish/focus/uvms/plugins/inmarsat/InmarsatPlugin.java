package fish.focus.uvms.plugins.inmarsat;

import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.ReportType;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.schema.exchange.registry.v1.ExchangeRegistryMethod;
import fish.focus.schema.exchange.service.v1.CapabilityListType;
import fish.focus.schema.exchange.service.v1.ServiceType;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.uvms.exchange.model.constant.ExchangeModelConstants;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.plugins.inmarsat.message.PluginMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.inject.Inject;
import javax.jms.JMSException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Startup
@Singleton
public class InmarsatPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPlugin.class);

    private static final int MAX_NUMBER_OF_TRIES = 20;
    private static final String PLUGIN_PROPERTIES = "plugin.properties";
    private static final String SETTINGS_PROPERTIES = "settings.properties";
    private static final String CAPABILITIES_PROPERTIES = "capabilities.properties";
    private final ConcurrentMap<String, String> capabilities = new ConcurrentHashMap<>();

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private HelperFunctions functions;

    @Inject
    private SettingsHandler settingsHandler;
    private boolean isRegistered = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName;
    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;
    private Properties twoStageApplicationProperties;
    private Properties twoStageProperties;
    private Properties twoStageCapabilities;

    private ConcurrentMap<String, String> getCapabilities() {
        return capabilities;
    }

    private Properties getPluginApplicationProperties() {
        return twoStageApplicationProperties;
    }

    private void setPluginApplicationProperties(Properties twostageApplicaitonProperties) {
        this.twoStageApplicationProperties = twostageApplicaitonProperties;
    }

    private Properties getPluginProperties() {
        return twoStageProperties;
    }

    private void setPluginProperties(Properties twostageProperties) {
        this.twoStageProperties = twostageProperties;
    }

    private Properties getPluginCapabilities() {
        return twoStageCapabilities;
    }

    private void setPluginCapabilities(Properties twoStageCapabilities) {
        this.twoStageCapabilities = twoStageCapabilities;
    }

    @PostConstruct
    private void startup() {
        Properties pluginProperties = functions.getPropertiesFromFile(this.getClass(), PLUGIN_PROPERTIES);
        setPluginApplicationProperties(pluginProperties);
        registerClassName = getPluginApplicationProperty("application.groupid");
        LOGGER.debug("Plugin will try to register as:{}", registerClassName);
        setPluginProperties(functions.getPropertiesFromFile(this.getClass(), SETTINGS_PROPERTIES));
        setPluginCapabilities(functions.getPropertiesFromFile(this.getClass(), CAPABILITIES_PROPERTIES));
        functions.mapToMapFromProperties(settingsHandler.getSettings(), getPluginProperties(), getRegisterClassName());
        functions.mapToMapFromProperties(getCapabilities(), getPluginCapabilities(), null);

        capabilityList = ServiceMapper.getCapabilitiesListTypeFromMap(getCapabilities());
        settingList = ServiceMapper.getSettingsListTypeFromMap(settingsHandler.getSettings());
        serviceType = ServiceMapper.getServiceType(getRegisterClassName(), "Thrane&Thrane",
                "inmarsat plugin for the Thrane&Thrane API", PluginType.SATELLITE_RECEIVER,
                getPluginResponseSubscriptionName(), "INMARSAT_C");

        register();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Settings updated in plugin {}", registerClassName);
            for (Map.Entry<String, String> entry : settingsHandler.getSettings().entrySet()) {
                LOGGER.debug("Setting: KEY: {} , VALUE: {}", entry.getKey(), entry.getValue());
            }
        }
        LOGGER.info("PLUGIN STARTED");
    }

    @PreDestroy
    private void shutdown() {
        unregister();
    }

    @Schedule(second = "*/10", minute = "*", hour = "*", persistent = false)
    private void timeout(Timer timer) {
        try {
            LOGGER.info("HEARTBEAT timeout running. isRegistered={} ,numberOfTriesExecuted={} threadId={}",
                    isRegistered, numberOfTriesExecuted, Thread.currentThread());
            if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
                LOGGER.info("{} is not registered, trying to register", getRegisterClassName());
                register();
                numberOfTriesExecuted++;
            }
            if (isRegistered) {
                LOGGER.info("{} is registered. Cancelling timer.", getRegisterClassName());
                timer.cancel();
            } else if (numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
                LOGGER.info("{} failed to register, maximum number of retries reached.", getRegisterClassName());
            }
        } catch (Exception e) {
            LOGGER.error(e.toString(), e);
        }
    }

    private void register() {
        LOGGER.info("Registering to Exchange Module");
        try {
            String registerServiceRequest = ExchangeModuleRequestMapper.createRegisterServiceRequest(serviceType, capabilityList, settingList);
            messageProducer.sendEventBusMessage(registerServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE,
                    ExchangeRegistryMethod.REGISTER_SERVICE.value());
            LOGGER.info("Registering to Exchange Module successfully sent.");
        } catch (JMSException | RuntimeException e) {
            LOGGER.error("Failed to send registration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    private void unregister() {
        LOGGER.info("Unregistering from Exchange Module");
        try {
            String unregisterServiceRequest = ExchangeModuleRequestMapper.createUnregisterServiceRequest(serviceType);
            messageProducer.sendEventBusMessage(unregisterServiceRequest, ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE,
                    ExchangeRegistryMethod.UNREGISTER_SERVICE.value());
        } catch (JMSException | RuntimeException e) {
            LOGGER.error("Failed to send unregistration message to {}", ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE);
        }
    }

    public String getPluginResponseSubscriptionName() {
        return getRegisterClassName() + "." + getPluginApplicationProperty("application.responseTopicName");
    }

    public String getRegisterClassName() {
        return registerClassName;
    }

    private String getPluginApplicationProperty(String key) {
        try {
            return (String) getPluginApplicationProperties().get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: {} {}", key, getRegisterClassName());
            return null;
        }
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
        LOGGER.info("setRegistered : {}", isRegistered);
        this.isRegistered = isRegistered;
    }

    public String getApplicationName() {
        try {
            return (String) getPluginApplicationProperties().get("application.name");
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: application.name: {}", getRegisterClassName());
            return null;
        }
    }

    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }
}
