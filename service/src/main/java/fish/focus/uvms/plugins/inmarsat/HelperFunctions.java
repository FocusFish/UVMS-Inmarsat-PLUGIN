package fish.focus.uvms.plugins.inmarsat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatSocketException;
import javax.ejb.Stateless;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

@Stateless
public class HelperFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger("InmarsatPlugin");

    public Properties getPropertiesFromFile(Class clazz, String fileName) {
        Properties props = new Properties();
        try {
            InputStream inputStream = clazz.getClassLoader().getResourceAsStream(fileName);
            props.load(inputStream);
        } catch (IOException e) {
            LOGGER.debug("Properties file failed to load");
        }
        return props;
    }

    public byte[] readStream(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int bytesRead = 0;
        while ((bytesRead = in.read(buffer)) > 0) {
            bos.write(buffer, 0, bytesRead);
        }
        return bos.toByteArray();
    }

    public void write(String value, PrintStream out) {
        out.println(value + "\r\n");
        out.flush();
    }

    public String readUntil(String pattern, InputStream in) throws InmarsatSocketException, IOException {
        StringBuilder sb = new StringBuilder();
        byte[] contents = new byte[1024];
        int bytesRead;

        do {
            bytesRead = in.read(contents);
            if (bytesRead > 0) {
                String s = new String(contents, 0, bytesRead);
                sb.append(s);
                String currentString = sb.toString();
                if (currentString.trim().endsWith(pattern)) {
                    return currentString;
                } else {
                    containsFault(currentString);
                }
            }
        } while (bytesRead >= 0);

        throw new InmarsatSocketException("Unknown response from Inmarsat-C LES Telnet @   (readUntil) : " + sb.toString());
    }

    public void containsFault(String currentString) throws InmarsatSocketException {
        for (String faultPattern : Constants.FAULT_PATTERNS) {
            if (currentString.trim().contains(faultPattern)) {
                throw new InmarsatSocketException(Constants.RESPONSE_IN_FAULT_PATTERN_ERROR_MESSAGE + currentString);
            }
        }
    }

    public void sendPwd(PrintStream output, String pwd) {
        output.print(pwd + "\r\n");
        output.flush();
    }

    public void mapToMapFromProperties(ConcurrentMap<String, String> map, Properties props, String registerClassName) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            if (entry.getKey().getClass().isAssignableFrom(String.class)) {
                String key = (String) entry.getKey();
                if (registerClassName != null) {
                    key = registerClassName.concat("." + key);
                }
                map.put(key, (String) entry.getValue());
            }
        }
    }
}
