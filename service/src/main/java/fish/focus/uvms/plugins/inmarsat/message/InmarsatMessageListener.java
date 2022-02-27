package fish.focus.uvms.plugins.inmarsat.message;

import fish.focus.schema.exchange.common.v1.AcknowledgeType;
import fish.focus.schema.exchange.common.v1.AcknowledgeTypeType;
import fish.focus.schema.exchange.common.v1.PollStatusAcknowledgeType;
import fish.focus.schema.exchange.module.v1.ExchangeModuleMethod;
import fish.focus.schema.exchange.movement.mobileterminal.v1.IdList;
import fish.focus.schema.exchange.movement.mobileterminal.v1.IdType;
import fish.focus.schema.exchange.movement.mobileterminal.v1.MobileTerminalId;
import fish.focus.schema.exchange.movement.v1.*;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.schema.exchange.v1.ExchangeLogStatusTypeType;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import fish.focus.uvms.commons.les.inmarsat.InmarsatException;
import fish.focus.uvms.commons.les.inmarsat.InmarsatInterpreter;
import fish.focus.uvms.commons.les.inmarsat.InmarsatMessage;
import fish.focus.uvms.commons.les.inmarsat.body.PositionReport;
import fish.focus.uvms.plugins.inmarsat.InmarsatPlugin;
import fish.focus.uvms.plugins.inmarsat.InmarsatPollHandler;
import fish.focus.uvms.plugins.inmarsat.PluginPendingResponseList;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPendingResponse;
import fish.focus.uvms.plugins.inmarsat.data.ModuleQueue;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/queue/UVMSInmarsatMessages"),
})
public class InmarsatMessageListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(InmarsatMessageListener.class);

    @Inject
    private InmarsatPlugin inmarsatPlugin;

    @Inject
    private InmarsatInterpreter inmarsatInterpreter;

    @Inject
    private InmarsatPollHandler inmarsatPollHandler;

    @Inject
    private PluginMessageProducer messageProducer;

    @Inject
    @Metric(name = "inmarsat_incoming", absolute = true)
    private Counter inmarsatIncoming;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                byte[] payload = message.getBody(byte[].class);
                if(payload != null){
                    InmarsatMessage[] inmarsatMessagesPerOceanRegion = inmarsatInterpreter.byteToInmMessage(payload);

                    if ((inmarsatMessagesPerOceanRegion != null) && (inmarsatMessagesPerOceanRegion.length > 0)) {
                        for (InmarsatMessage msg : inmarsatMessagesPerOceanRegion) {
                            LOG.info("Position from DNID: {}, memberNumber: {}", msg.getHeader().getDnid(), msg.getHeader().getMemberNo());
                            try {
                                msgToQue(msg, payload);
                                inmarsatIncoming.inc();
                            } catch (InmarsatException e) {
                                LOG.error("Positiondate not found in " + msg.toString(), e);
                            }
                        }
                    }
                }
            }
        } catch (JMSException e) {
            LOG.error(e.toString(), e);
        }
    }

    private void msgToQue(InmarsatMessage msg, byte[] messageAsBytes) throws InmarsatException {
        IdList dnidId = createDNIDIdList(msg);
        IdList membId = createMemberNumberIdList(msg);
        MovementBaseType movement = createMovementBaseType(msg, dnidId, membId);
        SetReportMovementType reportType = createSetReportMovementType(messageAsBytes, movement);

        PluginPendingResponseList responseList = inmarsatPollHandler.getPluginPendingResponseList();
        InmarsatPendingResponse ipr = responseList.containsPollFor(dnidId.getValue(), membId.getValue());

        if (ipr != null) {
            reportType.setPollRef(ipr.getMsgId());
        }

        if (!sendMovementReportToExchange(reportType)) {
            return; // return if sending fails
        }

        // If sending succeeded, check if it was a PollRequest. If so, update PollRequest status.
        // If the report is a pending poll response, also generate a status update for that poll

        if (ipr != null) {
            LOG.info("PendingPollResponse found in list: {}", ipr.getReferenceNumber());
            AcknowledgeType ackType = createSuccessfulAcknowledgeType(ipr);

            try {
                String s = ExchangePluginResponseMapper.mapToSetPollStatusToSuccessfulResponse(inmarsatPlugin.getApplicationName(), ackType, ipr.getMsgId());
                messageProducer.sendModuleMessage(s, ModuleQueue.EXCHANGE, ExchangeModuleMethod.PLUGIN_SET_COMMAND_ACK.value());
                boolean b = responseList.removePendingPollResponse(ipr);
                LOG.debug("Pending poll response removed: {}", b);
            } catch (RuntimeException ex) {
                LOG.debug("ExchangeModelMarshallException", ex);
            } catch (JMSException jex) {
                LOG.debug("JMSException", jex);
            }
        }
        LOG.debug("Sending movement to Exchange");
    }

    private IdList createDNIDIdList(InmarsatMessage msg) {
        IdList dnidId = new IdList();
        dnidId.setType(IdType.DNID);
        dnidId.setValue(Integer.toString(msg.getHeader().getDnid()));
        return dnidId;
    }

    private IdList createMemberNumberIdList(InmarsatMessage msg) {
        IdList membId = new IdList();
        membId.setType(IdType.MEMBER_NUMBER);
        membId.setValue(Integer.toString(msg.getHeader().getMemberNo()));
        return membId;
    }

    private MovementBaseType createMovementBaseType(InmarsatMessage msg, IdList dnidId, IdList membId) throws InmarsatException {
        MovementBaseType movement = new MovementBaseType();
        movement.setComChannelType(MovementComChannelType.MOBILE_TERMINAL);
        MobileTerminalId mobTermId = new MobileTerminalId();
        mobTermId.getMobileTerminalIdList().add(dnidId);
        mobTermId.getMobileTerminalIdList().add(membId);
        movement.setMobileTerminalId(mobTermId);
        movement.setMovementType(MovementTypeType.POS);
        MovementPoint mp = new MovementPoint();
        mp.setAltitude(0.0);
        mp.setLatitude(((PositionReport) msg.getBody()).getLatitude().getAsDouble());
        mp.setLongitude(((PositionReport) msg.getBody()).getLongitude().getAsDouble());
        movement.setPosition(mp);

        movement.setPositionTime(((PositionReport) msg.getBody()).getPositionDate().getDate());
        movement.setReportedCourse((double) ((PositionReport) msg.getBody()).getCourse());
        movement.setReportedSpeed(((PositionReport) msg.getBody()).getSpeed());
        movement.setSource(MovementSourceType.INMARSAT_C);
        movement.setStatus(Integer.toString(((PositionReport) msg.getBody()).getMacroEncodedMessage()));
        
        movement.setLesReportTime( msg.getHeader().getStoredTime() );
        movement.setSourceSatelliteId(msg.getHeader().getSatId() == null ? null : msg.getHeader().getSatId().getValue());
        
        return movement;
    }

    private SetReportMovementType createSetReportMovementType(byte[] messageAsBytes, MovementBaseType movement) {
        SetReportMovementType reportType = new SetReportMovementType();
        reportType.setMovement(movement);
        reportType.setTimestamp(new Date());
        reportType.setPluginName(inmarsatPlugin.getRegisterClassName());
        reportType.setPluginType(PluginType.SATELLITE_RECEIVER);

        reportType.setOriginalIncomingMessage(Base64.getEncoder().encodeToString(messageAsBytes));
        return reportType;
    }

    private AcknowledgeType createSuccessfulAcknowledgeType(InmarsatPendingResponse ipr) {
        AcknowledgeType ackType = new AcknowledgeType();
        ackType.setMessage("");

        PollStatusAcknowledgeType osat = new PollStatusAcknowledgeType();
        osat.setPollId(ipr.getMsgId());
        osat.setStatus(ExchangeLogStatusTypeType.SUCCESSFUL);

        ackType.setPollStatus(osat);
        ackType.setType(AcknowledgeTypeType.OK);

        String iprMessageId = ipr.getUnsentMsgId();
        ackType.setUnsentMessageGuid(iprMessageId);
        return ackType;
    }

    private boolean sendMovementReportToExchange(SetReportMovementType reportType) {
        try {
            String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(reportType, "TWOSTAGE",
                    null, Instant.now(),  PluginType.SATELLITE_RECEIVER, "TWOSTAGE", null);
            String messageId = messageProducer.sendModuleMessage(text, ModuleQueue.EXCHANGE, ExchangeModuleMethod.SET_MOVEMENT_REPORT.value());
            LOG.debug("Sent to exchange - text:{}, id:{}", text, messageId);
            return true;
        } catch (RuntimeException e) {
            LOG.error("Couldn't map movement to setreportmovementtype", e);
            return false;
        } catch (JMSException e) {
            LOG.error("couldn't send movement", e);
            return false;
        }
    }
}
