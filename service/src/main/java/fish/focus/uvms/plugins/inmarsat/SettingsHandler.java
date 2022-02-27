package fish.focus.uvms.plugins.inmarsat;

import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.schema.exchange.service.v1.SettingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SettingsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollHandler.class);

    private ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, String> getSettingsWithShortKeyNames() {
        ConcurrentHashMap<String, String> mapWithShortKeyNames = new ConcurrentHashMap<>();
        for (Map.Entry<String, String> kv : settings.entrySet()) {
            String key = kv.getKey();
            int pos = key.lastIndexOf(".");
            key = key.substring(pos + 1);
            String value = kv.getValue();

            if (key.equals("PORT")) {
                try {
                    Integer port = Integer.parseInt(value);
                    mapWithShortKeyNames.put(key, String.valueOf(port));
                } catch (NumberFormatException e) {
                    LOGGER.error("Port is not an integer");
                    return null;
                }
            } else {
                mapWithShortKeyNames.put(key, kv.getValue());
            }
        }
        return mapWithShortKeyNames;
    }

    public void updateSettings(List<SettingType> settings) {
        for (SettingType setting : settings) {
            LOGGER.info("Updating setting: {} = {}", setting.getKey(), setting.getValue());
            this.settings.put(setting.getKey(), setting.getValue());
        }
    }

    public String getSetting(String setting, String registerClassName) {
        LOGGER.debug("Trying to get setting {}.{}", registerClassName, setting);
        String settingValue = settings.get(registerClassName + "." + setting);
        LOGGER.debug("Got setting value for {}.{};{}", registerClassName, setting, settingValue);
        return settingValue;
    }

    public ConcurrentHashMap<String, String> getSettings() {
        return settings;
    }

    /**
     * Set the config values for the twostage
     *
     * @param settings the settings
     * @return AcknowledgeTypeType
     */
    public AcknowledgeTypeType setConfig(SettingListType settings, String registerClassName) {
        LOGGER.info(registerClassName + ".setConfig()");
        try {
            for (KeyValueType values : settings.getSetting()) {
                LOGGER.debug("Setting [ " + values.getKey() + " : " + values.getValue() + " ]");
                this.settings.put(values.getKey(), values.getValue());
            }
            return AcknowledgeTypeType.OK;
        } catch (Exception e) {
            LOGGER.error("Failed to set config in {}", registerClassName);
            return AcknowledgeTypeType.NOK;
        }
    }
}
