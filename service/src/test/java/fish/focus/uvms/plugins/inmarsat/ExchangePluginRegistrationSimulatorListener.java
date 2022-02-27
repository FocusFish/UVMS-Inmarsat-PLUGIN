package fish.focus.uvms.plugins.inmarsat;


import fish.focus.schema.exchange.common.v1.AcknowledgeType;
import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.CommandType;
import fish.focus.schema.exchange.module.v1.SetCommandRequest;
import fish.focus.schema.exchange.plugin.v1.StartRequest;
import fish.focus.schema.exchange.registry.v1.ExchangeRegistryBaseRequest;
import fish.focus.schema.exchange.registry.v1.RegisterServiceRequest;
import fish.focus.schema.exchange.registry.v1.UnregisterServiceRequest;
import fish.focus.schema.exchange.service.v1.ServiceResponseType;
import fish.focus.schema.exchange.service.v1.SettingListType;
import fish.focus.uvms.commons.message.api.MessageConstants;
import fish.focus.uvms.exchange.model.constant.ExchangeModelConstants;
import fish.focus.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import fish.focus.uvms.exchange.model.mapper.JAXBMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;



@MessageDriven(mappedName = MessageConstants.EVENT_BUS_TOPIC, activationConfig = {
        @ActivationConfigProperty(propertyName = MessageConstants.MESSAGE_SELECTOR_STR, propertyValue = ExchangeModelConstants.SERVICE_NAME + " = '" + ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE + "'"),
        @ActivationConfigProperty(propertyName = MessageConstants.MESSAGING_TYPE_STR, propertyValue = MessageConstants.CONNECTION_TYPE),
        @ActivationConfigProperty(propertyName = MessageConstants.SUBSCRIPTION_DURABILITY_STR, propertyValue = MessageConstants.DURABLE_CONNECTION),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_TYPE_STR, propertyValue = MessageConstants.DESTINATION_TYPE_TOPIC),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_STR, propertyValue = MessageConstants.EVENT_BUS_TOPIC_NAME),
        @ActivationConfigProperty(propertyName = MessageConstants.DESTINATION_JNDI_NAME, propertyValue = MessageConstants.EVENT_BUS_TOPIC),
        @ActivationConfigProperty(propertyName = MessageConstants.SUBSCRIPTION_NAME_STR, propertyValue = ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE),
        @ActivationConfigProperty(propertyName = MessageConstants.CLIENT_ID_STR, propertyValue = ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE),
        @ActivationConfigProperty(propertyName = MessageConstants.CONNECTION_FACTORY_JNDI_NAME, propertyValue = MessageConstants.CONNECTION_FACTORY)
})
public class ExchangePluginRegistrationSimulatorListener implements MessageListener {

    final static Logger LOG = LoggerFactory.getLogger(ExchangePluginRegistrationSimulatorListener.class);


    PluginMessageProducerForSimulator producer = new PluginMessageProducerForSimulator();



    @Override
    public void onMessage(Message message) {

        TextMessage textMessage = (TextMessage) message;
        try {
            ExchangeRegistryBaseRequest request = JAXBMarshaller.unmarshallTextMessage(textMessage, ExchangeRegistryBaseRequest.class);
            switch (request.getMethod()) {
                case REGISTER_SERVICE:
                    RegisterServiceRequest register = JAXBMarshaller.unmarshallTextMessage(textMessage, RegisterServiceRequest.class);
                    ServiceResponseType service = new ServiceResponseType();
                    SettingListType settingListType = new SettingListType();
                    service.setSettingList(settingListType);
                    String response = ExchangePluginResponseMapper.mapToRegisterServiceResponseOK("messageID", service);
                    //producer.sendEventBusMessage(response, register.getService().getServiceResponseMessageName());

                    break;
                case UNREGISTER_SERVICE:
                    UnregisterServiceRequest unRegReq = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceRequest.class);
                    unregisterService(textMessage);
                    break;
                default:
                    throw new Exception("[ Not implemented method consumed: " + request.getMethod() + " ]");
            }
        } catch (Exception  e) {
            LOG.error("[ Error when receiving message on topic in exchange: {}] {}",message,e);
            // CRAP errorEvent.fire(new PluginMessageEvent(textMessage, settings, ExchangePluginResponseMapper.mapToPluginFaultResponse(FaultCode.EXCHANGE_TOPIC_MESSAGE.getCode(), "Error when receiving message in exchange " + e.getMessage())));
        }
    }

    private void unregisterService(TextMessage textMessage) {
        try {
            UnregisterServiceRequest unregister = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceRequest.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }

    }










}





