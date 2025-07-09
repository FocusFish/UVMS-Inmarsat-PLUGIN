package fish.focus.uvms.plugins.inmarsat;

import fish.focus.schema.exchange.common.v1.CommandType;
import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.schema.exchange.plugin.types.v1.PollTypeType;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPendingResponse;
import fish.focus.uvms.plugins.inmarsat.data.PollResponse;
import fish.focus.uvms.plugins.inmarsat.data.StatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inmarsat har ikke definert Polling&Data rapport tjenesten hundre prosent, med det mener jeg at terminal leverandører
 * har hatt mulighet til å lage sine "tolkninger" på endel punkter. Hvis du ser på data rapport pakken (PosRep-C_2l.doc)
 * så ser du at nederst i pakke en er MEM-code. Den mest brukte MEM code er 11 I attribute bytene nederst i pakke en
 * vil mem 11 angi at atribute bytene inneholder  «time of position».
 * <p>
 * Angående scheduled programming. Legger med noe veldig gammel informasjon jeg laget på gruppe programmering:
 * <p>
 * First you have to program the terminals to send data report, and afterwards you have to start the reporting.
 * You have to send 2 poll commands to get the mobiles to report scheduled
 * <p>
 * 1)  First you have to send the program command:
 * If you want the report to start at e.g. 13:28 UTC today you have to send the following command:
 * <p>
 * poll 0,G,4661,N,1,0,4,,5611,24
 * The P8 parameter is the start frame. The start time must be calculated according to the formula:
 * (((hour*60)+minute)*60)/8.64 = start frame number. (night and day (24 hours) are divided in  10000 frame's a'8.64 sec.).
 * The "24" at the end indicate that the terminals shall send one report every hour.
 * <p>
 * 2)  Afterwards you have to start the reporting command:
 * e.g:  poll 0,G,4661,D,1,0,5
 * <p>
 * 3)  TO STOP THE REPORTING.
 * The data reporting are stopped using the command
 * e.g.: poll 0,G,4661,N,1,0,6
 */
@Singleton
public class InmarsatPollHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InmarsatPollHandler.class);

    @Inject
    private PluginPendingResponseList responseList;

    @Inject
    private HelperFunctions functions;

    @Inject
    private SettingsHandler settingsHandler;

    @Inject
    private PollSender pollSender;

    public PluginPendingResponseList getPluginPendingResponseList() {
        return responseList;
    }

    public PollResponse processCommandTypeAndReturnAck(CommandType command) {
        PollResponse response = new PollResponse();
        PollType pollType = command.getPoll();
        try {
            response = sendPoll(pollType);
            if (response.getReference() == null) {
                LOGGER.error("Error no reference for poll with pollId: {}", pollType.getPollId());
                return response; //consumed but erroneous
            }
            LOGGER.info("sent poll with pollId: {} and reference: {} ", pollType.getPollId(), response.getReference());

            if (pollType.getPollTypeType() == PollTypeType.POLL) {
                constructIPRAndAddInPPRL(command, response.getReference());
            }
            return response;
        } catch (Throwable e) {
            LOGGER.error("Error while sending poll: {}", e.getMessage(), e);
            response.setMessage("Error while sending poll: " + e);
        }
        return response;
    }

    private void constructIPRAndAddInPPRL(CommandType command, String reference) {
        PollType pollType = command.getPoll();
        InmarsatPendingResponse ipr = createAnInmarsatPendingResponseObject(pollType, reference);
        ipr.setUnsentMsgId(command.getUnsentMessageGuid());
        responseList.addPendingPollResponse(ipr);
    }

    private PollResponse sendPoll(PollType poll) {
        PollResponse result = new PollResponse();
        ConcurrentHashMap<String, String> settings = settingsHandler.getSettingsWithShortKeyNames();
        String url = settings.get("URL");
        int port = Integer.parseInt(settings.get("PORT"));
        String user = settings.get("USERNAME");
        String pwd = settings.get("PSW");

        List<String> wrkOceanRegions = getOceanRegions(poll);

        if (wrkOceanRegions.isEmpty()) {
            LOGGER.error("No Ocean Region in request. Check MobileTerminal. No Poll can be executed.");
            result.setMessage("No Ocean Region in request. Check MobileTerminal. No Poll can be executed.");
            return result;
        }

        Socket socket = null;

        PrintStream output = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(url, port), Constants.SOCKET_TIMEOUT);
            socket.setSoTimeout(Constants.SOCKET_TIMEOUT);
            // Logon
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            output = new PrintStream(socket.getOutputStream());
            functions.readUntil("name:", input);
            functions.write(user, output);
            functions.readUntil("word:", input);
            functions.sendPwd(output, pwd);
            functions.readUntil(">", input);


            // Sends one or more poll commands, one for each ocean region, until a reference number is received.
            for (String oceanRegion : wrkOceanRegions) {
                result = pollSender.sendPollCommand(poll, input, output, oceanRegion);
                if (result.getReference() != null) {
                    LOGGER.info("sendPoll invoked. Reference number : {} ", result.getReference());
                    return result;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("SEND POLL ERROR! pollId: {}", poll.getPollId());
            LOGGER.error(t.toString(), t);
            result.setMessage("Error sending poll: " + t);
        } finally {
            if (output != null)
                functions.write("QUIT", output);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    LOGGER.warn(e.toString(), e);
                }
            }
        }
        return result;
    }

    private List<String> getOceanRegions(PollType poll) {
        List<KeyValueType> pollReceiver = poll.getPollReceiver();
        List<String> wrkOceanRegions = new ArrayList<>();
        for (KeyValueType value : pollReceiver) {
            String key = value.getKey();
            if (key.equalsIgnoreCase("OCEAN_REGION")) {
                String wrkOceanRegionsStr = value.getValue() == null ? "" : value.getValue().trim();
                wrkOceanRegions.add(wrkOceanRegionsStr);
            }
        }
        return wrkOceanRegions;
    }

    private InmarsatPendingResponse createAnInmarsatPendingResponseObject(PollType poll, String result) {
        int refNumber = -1;
        try {
            refNumber = Integer.parseInt(result);
        } catch (NumberFormatException nfe) {
            LOGGER.error("Reference is not a number. Code should not reach this point. Erroneous usage of function");
        }

        // Register response as pending
        InmarsatPendingResponse ipr = new InmarsatPendingResponse();
        ipr.setPollType(poll);
        ipr.setMsgId(poll.getPollId());
        ipr.setReferenceNumber(refNumber);
        List<KeyValueType> pollReceiver = poll.getPollReceiver();
        for (KeyValueType element : pollReceiver) {
            if (element.getKey().equalsIgnoreCase("MOBILE_TERMINAL_ID")) {
                ipr.setMobTermId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("DNID")) {
                ipr.setDnId(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                ipr.setMembId(element.getValue());
            }
        }
        ipr.setStatus(StatusEnum.PENDING);
        ipr.setCreatedAt(Instant.now());
        return ipr;
    }
}

