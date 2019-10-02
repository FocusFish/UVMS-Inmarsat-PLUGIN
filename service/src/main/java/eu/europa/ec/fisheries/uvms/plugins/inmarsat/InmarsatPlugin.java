package eu.europa.ec.fisheries.uvms.plugins.inmarsat;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.ReportType;
import eu.europa.ec.fisheries.schema.exchange.module.v1.ExchangeModuleMethod;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.schema.exchange.registry.v1.ExchangeRegistryMethod;
import eu.europa.ec.fisheries.schema.exchange.service.v1.CapabilityListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.ServiceType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingListType;
import eu.europa.ec.fisheries.schema.exchange.service.v1.SettingType;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.plugins.inmarsat.message.PluginMessageProducer;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Startup
@Singleton
public class InmarsatPlugin  {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPlugin.class);

    private static final int MAX_NUMBER_OF_TRIES = 20;
    private boolean isRegistered = false;
    private int numberOfTriesExecuted = 0;
    private String registerClassName;
    private CapabilityListType capabilityList;
    private SettingListType settingList;
    private ServiceType serviceType;
    private static final String PLUGIN_PROPERTIES = "plugin.properties";
    private static final String SETTINGS_PROPERTIES = "settings.properties";
    private static final String CAPABILITIES_PROPERTIES = "capabilities.properties";
    private final ConcurrentMap<String, String> capabilities = new ConcurrentHashMap<>();
    private Properties twostageApplicationProperties;
    private Properties twostageProperties;
    private Properties twostageCapabilities;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    private HelperFunctions functions;

    @Inject SettingsHandler settingsHandler;

    private ConcurrentMap<String, String> getCapabilities() {
        return capabilities;
    }

    private Properties getPluginApplicationProperties() {
        return twostageApplicationProperties;
    }

    private void setPluginApplicationProperties(Properties twostageApplicaitonProperties) {
        this.twostageApplicationProperties = twostageApplicaitonProperties;
    }

    private Properties getPluginProperties() {
        return twostageProperties;
    }

    private void setPluginProperties(Properties twostageProperties) {
        this.twostageProperties = twostageProperties;
    }

    private Properties getPluginCapabilities() {
        return twostageCapabilities;
    }

    private void setPluginCapabilities(Properties twostageCapabilities) {
        this.twostageCapabilities = twostageCapabilities;
    }

    @PostConstruct
    private void startup() {
        Properties pluginProperties = functions.getPropertiesFromFile(this.getClass(), PLUGIN_PROPERTIES);
        setPluginApplicationProperties(pluginProperties);
        registerClassName = getPluginApplicationProperty("application.groupid") + "." + getPluginApplicationProperty("application.name");
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
            LOGGER.info("HEARTBEAT timeout running. isRegistered=" + isRegistered +
                    " ,numberOfTriesExecuted=" + numberOfTriesExecuted + " threadId=" + Thread.currentThread().toString());
            if (!isRegistered && numberOfTriesExecuted < MAX_NUMBER_OF_TRIES) {
                LOGGER.info(getRegisterClassName() + " is not registered, trying to register");
                register();
                numberOfTriesExecuted++;
            }
            if (isRegistered) {
                LOGGER.info(getRegisterClassName() + " is registered. Cancelling timer.");
                timer.cancel();
            } else if (numberOfTriesExecuted >= MAX_NUMBER_OF_TRIES) {
                LOGGER.info(getRegisterClassName() + " failed to register, maximum number of retries reached.");
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
            LOGGER.error("Failed to getSetting for key: " + key, getRegisterClassName());
            return null;
        }
    }

    public boolean isIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(boolean isRegistered) {
        LOGGER.info("setRegistered : " + isRegistered);
        this.isRegistered = isRegistered;
    }

    public String getApplicationName() {
        try {
            return (String) getPluginApplicationProperties().get("application.name");
        } catch (Exception e) {
            LOGGER.error("Failed to getSetting for key: application.name", getRegisterClassName());
            return null;
        }
    }

    private boolean sendMovementReportToExchange(SetReportMovementType reportType) {
        try {
            String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE",
                    null, Instant.now(),  PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
            String messageId = messageProducer.sendModuleMessage(text, ModuleQueue.EXCHANGE, ExchangeModuleMethod.SET_MOVEMENT_REPORT.value());
            LOGGER.debug("Sent to exchange - text:{}, id:{}", text, messageId);
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't map movement to setreportmovementtype", e);
            return false;
        } catch (JMSException e) {
            LOGGER.error("couldn't send movement", e);
            return false;
        }
    }

    public AcknowledgeTypeType setReport(ReportType report) {
        return AcknowledgeTypeType.NOK;
    }

}
