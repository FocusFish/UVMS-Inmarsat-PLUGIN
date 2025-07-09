package fish.focus.uvms.commons.les.inmarsat;


import fish.focus.uvms.commons.les.inmarsat.header.HeaderDataPresentation;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderStruct;
import fish.focus.uvms.commons.les.inmarsat.header.HeaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

@Stateless
public class InmarsatInterpreter {
    private static final String INMARSAT_FAILED_REPORT_QUEUE = "jms/queue/UVMSPluginFailedReport";
    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatInterpreter.class);
    private static final byte[] HEADER_PATTERN = ByteBuffer.allocate(4).put((byte) InmarsatDefinition.API_SOH)
            .put(InmarsatDefinition.API_LEAD_TEXT.getBytes()).array();
    private static final int PATTERN_LENGTH = HEADER_PATTERN.length;

    @Resource(mappedName = "java:/" + INMARSAT_FAILED_REPORT_QUEUE)
    private Queue inmarsatFailedReportQueue;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    public InmarsatMessage[] byteToInmMessage(final byte[] fileBytes) {
        byte[] bytes = insertMissingData(fileBytes);

        ArrayList<InmarsatMessage> messages = new ArrayList<>();
        if (bytes == null || bytes.length <= PATTERN_LENGTH) {
            LOGGER.error("Not a valid Inmarsat Message: {}", Arrays.toString(bytes));
            return new InmarsatMessage[]{};
        }
        // Parse bytes for messages
        for (int i = 0; i < (bytes.length - PATTERN_LENGTH); i++) {
            // Find message
            if (InmarsatHeader.isStartOfMessage(bytes, i)) {
                InmarsatMessage message;
                byte[] messageBytes = Arrays.copyOfRange(bytes, i, bytes.length);
                try {
                    message = new InmarsatMessage(messageBytes);
                } catch (InmarsatException e) {
                    LOGGER.error(e.toString(), e);
                    try {
                        sendFailedReportMessage(messageBytes, fileBytes, e);
                    } catch (JMSException ee) {
                        LOGGER.error("could not post rejected message to queue : {}", Arrays.toString(messageBytes));
                    }
                    continue;
                }

                if (message.validate()) {
                    messages.add(message);
                } else {
                    LOGGER.error("Could not validate position(s)");
                    try {
                        sendFailedReportMessage(messageBytes, fileBytes, null);
                    } catch (JMSException e) {
                        LOGGER.error("could not post rejected message to queue : {}", Arrays.toString(messageBytes));
                    }
                }
            }
        }

        return messages.toArray(new InmarsatMessage[0]);
    }

    public String sendFailedReportMessage(byte[] incomingMessage, byte[] originalMessage, Exception exception) throws JMSException {
        // we slice it up because buffersize is limited to 50000isch in Artemis
        if (incomingMessage.length > 50000) {
            byte[] truncated = Arrays.copyOfRange(incomingMessage, 0, 50000);
            sendFailedReportMessage(truncated, originalMessage, exception);
            incomingMessage = Arrays.copyOfRange(incomingMessage, 50000, incomingMessage.length);
        }

        String messageStr = byte2str(incomingMessage);
        String exceptionStr = "";
        if (exception != null) {
            exceptionStr = exception.toString();
        }

        if (connectionFactory == null) {
            LOGGER.warn("could not send errormsg - you are probably in test");
            return "not send";
        }

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(inmarsatFailedReportQueue);
        ) {

            TextMessage message = session.createTextMessage();
            message.setStringProperty("messagesource", "INMARSAT_C");
            message.setStringProperty("message_as_hex_string", messageStr);
            String padded64String = "Padded message as base64string: " + Base64.getEncoder().encodeToString(incomingMessage) + "\r\n";
            String base64String = "Base message as base64string: " + Base64.getEncoder().encodeToString(originalMessage);
            if (exception != null) {
                message.setStringProperty("exception", exceptionStr);
            }
            message.setText(padded64String + base64String);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);


            return message.getJMSMessageID();
        } catch (JMSException e) {
            throw e;
        }
    }

    private String byte2str(byte[] message) {
        if (message == null) return "";
        if (message.length < 1) return "";

        StringBuilder sb = new StringBuilder(message.length * 2);
        for (byte b : message) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Header sent doesn't always adhere to the byte contract.. This method tries to insert fix the missing parts..
     *
     * @param input bytes that might contain miss some bytes
     * @return message with fixed bytes
     */
    public byte[] insertMissingData(byte[] input) {
        byte[] output = insertMissingMsgRefNo(input);
        output = insertMissingMsgRefNo(output);         //incoming inmarsat message might be missing one or two bytes in the message reference number
        //output = insertMissingStoredTime(output);
        output = insertMissingMemberNo(output);
        output = padHeaderToCorrectLength(output);

        if (input.length < output.length) {
            LOGGER.warn("Message fixed: {} -> {}", InmarsatUtils.bytesArrayToHexString(input),
                    InmarsatUtils.bytesArrayToHexString(output));
        }

        return output;
    }

    private byte[] padHeaderToCorrectLength(final byte[] contents) {
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean insert = false, doubleInsert = false;
        int insertPosition = 0;
        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                byte[] header = Arrays.copyOfRange(input, i, input.length);
                HeaderType headerType = InmarsatHeader.getType(header);

                int headerLength = headerType.getHeaderLength();
                int realHeaderLength = header.length;
                if ((headerLength - 1) < realHeaderLength) {  // avoid arrayoutofbounds
                    int token = header[headerLength - 1];
                    if (token != InmarsatDefinition.API_EOH) {
                        if (header[headerLength - 2] == InmarsatDefinition.API_EOH) {
                            insert = true;
                            insertPosition = i + headerType.getHeaderStruct().getPositionStoredTime();
                        } else if (header[headerLength - 3] == InmarsatDefinition.API_EOH) {
                            doubleInsert = true;
                            insertPosition = i + headerType.getHeaderStruct().getPositionStoredTime();
                        }
                        LOGGER.warn("Header is {} short so we add 00 as needed to the stored time position at position: {}", (doubleInsert) ? "two" : "one", insertPosition);    //since we dont use stored time it is "relatively" risk-free to add positions there

                    }
                }
            }
            if ((insert || doubleInsert) && ((insertPosition) == i)) {
                insert = false;
                insertPosition = 0;
                output.write((byte) InmarsatDefinition.API_UNKNOWN_ERROR); // Just 00
                if (doubleInsert) {
                    doubleInsert = false;
                    output.write((byte) InmarsatDefinition.API_UNKNOWN_ERROR); // One more 00
                }
            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }

    private byte[] insertMissingMsgRefNo(final byte[] contents) {
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // Missing last MsgRefNo- insert #00 in before presentation..
        boolean insert = false;
        int insertPosition = 0;
        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                byte[] header = Arrays.copyOfRange(input, i, input.length);
                HeaderType headerType = InmarsatHeader.getType(header);

                if (headerType.getHeaderStruct().isPresentation()) {
                    HeaderDataPresentation presentation = InmarsatHeader.getDataPresentation(header);   //Data presentation checks if position 11 is a one or a two

                    if (presentation == null) {
                        insert = true;
                        insertPosition = i + HeaderStruct.POS_REF_NO_START;
                        LOGGER.warn("Presentation is not correct so we add 00 to msg ref no at position: {}", insertPosition);
                    }
                }
            }
            if (insert && (insertPosition == i)) {
                insert = false;
                insertPosition = 0;
                output.write((byte) 0x00);
            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }

    private byte[] insertMissingMemberNo(byte[] contents) {
        // Missing Member number - insert #FF before EOH
        // continue from previous cleaned data
        byte[] input = contents.clone();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean insert = false;
        int insertPosition = 0;

        for (int i = 0; i < input.length; i++) {
            // Find SOH
            if (InmarsatHeader.isStartOfMessage(input, i)) {
                int headerLength = input[i + HeaderStruct.POS_HEADER_LENGTH];
                int expectedEOHPosition = i + headerLength - 1;
                // Check if memberNo exits
                if ((expectedEOHPosition >= input.length)
                        || ((input[expectedEOHPosition - 1] == (byte) InmarsatDefinition.API_EOH)
                        && input[expectedEOHPosition] != (byte) InmarsatDefinition.API_EOH)) {
                    insert = true;
                    insertPosition = expectedEOHPosition - 1;
                }

            }
            // Find EOH
            if (insert && (input[i] == (byte) InmarsatDefinition.API_EOH) && (insertPosition == i)) {
                output.write((byte) 0xFF);
                insert = false;
                insertPosition = 0;
                LOGGER.warn("Message is missing member no, inserting FF at position: {}", insertPosition);
            }
            output.write(input[i]);
        }
        return output.toByteArray();
    }
}
