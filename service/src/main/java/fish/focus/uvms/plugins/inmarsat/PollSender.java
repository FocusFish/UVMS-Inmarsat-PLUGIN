package fish.focus.uvms.plugins.inmarsat;

import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.uvms.plugins.inmarsat.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Stateless
public class PollSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollSender.class);

    @Inject
    private HelperFunctions functions;

    /**
     * Sends one or more poll commands, one for each ocean region, until a reference number is
     * received.
     *
     * @return result of first successful poll command, or null if poll failed on every ocean region
     */

    public PollResponse sendPollCommand(PollType pollType, BufferedInputStream in, PrintStream out, String oceanRegion) {
        InmarsatPoll poll = getPoll(pollType, oceanRegion);
        List<String> pollCommandList = poll.asCommand();
        PollResponse result = new PollResponse();
        try  {
            for (String pollCommand : pollCommandList) {
                LOGGER.info(pollCommand);
                result = sendPollCommand(in, out, pollCommand);
                if (result.getReference() == null) {
                    LOGGER.info("NO  referencenumber. Message not send");
                }
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        } catch (IOException | InmarsatSocketException e) {
            LOGGER.error(e.toString(), e);
            result.setMessage("Error sending poll: " + stripErrorMessageOfException("" + e));
        }
        return result;
    }

    private InmarsatPoll getPoll(PollType pollType, String oceanRegion) {
        InmarsatPoll poll = null;
        switch (pollType.getPollTypeType()) {
            case POLL:
                poll = new ManualPoll(oceanRegion);
                poll.setFieldsFromPollRequest(pollType);
                break;
            case CONFIG:
                poll = new ConfigPoll(oceanRegion);
                poll.setFieldsFromPollRequest(pollType);
                break;
            case SAMPLING:
                break;
        }
        return poll;
    }

    private PollResponse sendPollCommand(BufferedInputStream bis, PrintStream out, String cmd) throws InmarsatSocketException, IOException {
        PollResponse response = new PollResponse();
        functions.write(cmd, out);
        functions.readUntil("Text:", bis);
        functions.write(".s", out);
        String status = functions.readUntil(">", bis);
        status = stripUnimportantParts(status);
        response.setMessage(status);
        response.setReference(toReferenceNumber(status));
        LOGGER.info("Status Number: {}", status);
        return response;

    }

    private static final String END_OF_COMMON_PART = "...";

    private String stripUnimportantParts(String response){
        int pos = response.indexOf(END_OF_COMMON_PART);
        if (pos < 0) return response;
        return response.substring(pos + END_OF_COMMON_PART.length(), response.length() - 2).trim();  //-2 for removing the trailing >
    }

    private String toReferenceNumber(String response) {
        int pos = response.indexOf("number");
        String reference;
        if (pos < 0) return null;
        reference = response.substring(pos);
        return reference.replaceAll("[^0-9]", ""); // returns 123
    }

    private String stripErrorMessageOfException(String message){
        int index = message.indexOf(Constants.RESPONSE_IN_FAULT_PATTERN_ERROR_MESSAGE);
        if (index < 0) return message;
        return message.substring(index + Constants.RESPONSE_IN_FAULT_PATTERN_ERROR_MESSAGE.length(), message.length() - 2).trim();
    }
}
