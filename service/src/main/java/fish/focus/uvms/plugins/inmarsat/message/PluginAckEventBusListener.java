/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.plugins.inmarsat.message;

import fish.focus.schema.exchange.registry.v1.ExchangeRegistryBaseRequest;
import fish.focus.schema.exchange.registry.v1.RegisterServiceResponse;
import fish.focus.schema.exchange.registry.v1.UnregisterServiceResponse;
import fish.focus.uvms.exchange.model.mapper.JAXBMarshaller;
import fish.focus.uvms.plugins.inmarsat.InmarsatPlugin;
import fish.focus.uvms.plugins.inmarsat.SettingsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@MessageDriven(activationConfig =  {
        @ActivationConfigProperty(propertyName = "subscriptionName",          propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE"),
        @ActivationConfigProperty(propertyName = "clientId",                  propertyValue = "eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE"),
        @ActivationConfigProperty(propertyName = "messageSelector",           propertyValue = "ServiceName='eu.europa.ec.fisheries.uvms.plugins.inmarsat.PLUGIN_RESPONSE'"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability",    propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "destinationLookup",         propertyValue = "jms/topic/EventBus"),
        @ActivationConfigProperty(propertyName = "destinationType",           propertyValue = "javax.jms.Topic")
})
public class PluginAckEventBusListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginAckEventBusListener.class);

    @Inject
    private InmarsatPlugin startupService;

    @Inject
    private SettingsHandler settingsHandler;

    @Override
    public void onMessage(Message inMessage) {
        LOGGER.info("Eventbus listener for twostage at selector: {} got a message", startupService.getPluginResponseSubscriptionName());
        TextMessage textMessage = (TextMessage) inMessage;

        try {
            ExchangeRegistryBaseRequest request = tryConsumeRegistryBaseRequest(textMessage);
            if (request == null) {
                handlePluginFault(textMessage);
            } else {
                switch (request.getMethod()) {
                    case REGISTER_SERVICE:
                        RegisterServiceResponse registerResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, RegisterServiceResponse.class);
                        switch (registerResponse.getAck().getType()) {
                            case OK:
                                LOGGER.info("Register OK");
                                startupService.setIsRegistered(Boolean.TRUE);
                                settingsHandler.updateSettings(registerResponse.getService().getSettingList().getSetting());
                                break;
                            case NOK:
                                LOGGER.info("Register NOK: " + registerResponse.getAck().getMessage());
                                startupService.setIsRegistered(Boolean.FALSE);
                                break;
                            default:
                                LOGGER.error("[ Type not supperted: ]" + request.getMethod());
                        }
                        break;
                    case UNREGISTER_SERVICE:
                        UnregisterServiceResponse unregisterResponse = JAXBMarshaller.unmarshallTextMessage(textMessage, UnregisterServiceResponse.class);
                        switch (unregisterResponse.getAck().getType()) {
                            case OK:
                                LOGGER.info("Unregister OK");
                                break;
                            case NOK:
                                LOGGER.info("Unregister NOK");
                                break;
                            default:
                                LOGGER.error("[ Ack type not supported ] ");
                                break;
                        }
                        break;
                    default:
                        LOGGER.error("Not supported method");
                        break;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("[ Error when receiving message in twostage ]", e);
        }
    }

    private void handlePluginFault(TextMessage fault) {
        try {
            LOGGER.error(
                    startupService.getPluginResponseSubscriptionName() + " received fault : " + fault.getText() + " : " );
        } catch (JMSException e) {
            LOGGER.error("Could not get text from incoming message in inmarsat-c");
        }
    }

    private ExchangeRegistryBaseRequest tryConsumeRegistryBaseRequest(TextMessage textMessage) {
        try {
            return JAXBMarshaller.unmarshallTextMessage(textMessage, ExchangeRegistryBaseRequest.class);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
