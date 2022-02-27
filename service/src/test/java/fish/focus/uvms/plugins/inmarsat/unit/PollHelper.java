package fish.focus.uvms.plugins.inmarsat.unit;

import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.schema.exchange.plugin.types.v1.PollTypeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PollHelper {

    final String OCEAN_REGION = "1";
    final int DNID = 12345;
    final int ADDRESS = 412345678;
    final int MEMBER_NUMBER = 101;
    final int REPORT_FREQUENCY = 7200; // 02:00 as seconds
    final int START_FRAME = 48600; // 13:30 as seconds
    final int IN_PORT_GRACE = 54000; // 15:00 as seconds

    PollType createConfigPoll() {
        KeyValueType dnid = new KeyValueType();
        dnid.setKey("DNID"); dnid.setValue(String.valueOf(DNID));
        KeyValueType address = new KeyValueType();
        address.setKey("SATELLITE_NUMBER"); address.setValue(String.valueOf(ADDRESS));
        KeyValueType memberNumber = new KeyValueType();
        memberNumber.setKey("MEMBER_NUMBER"); memberNumber.setValue(String.valueOf(MEMBER_NUMBER));
        KeyValueType frequency = new KeyValueType();
        frequency.setKey("REPORT_FREQUENCY"); frequency.setValue(String.valueOf(REPORT_FREQUENCY));
        KeyValueType startFrame = new KeyValueType();
        startFrame.setKey("GRACE_PERIOD"); startFrame.setValue(String.valueOf(START_FRAME));
        KeyValueType inPortGrace = new KeyValueType();
        inPortGrace.setKey("IN_PORT_GRACE"); inPortGrace.setValue(String.valueOf(IN_PORT_GRACE));

        List<KeyValueType> pollReceiver = new ArrayList<>(Arrays.asList(dnid, address, memberNumber));

        List<KeyValueType> pollPayload = new ArrayList<>(Arrays.asList(frequency, startFrame, inPortGrace));

        PollType poll = new PollType();
        poll.setPollTypeType(PollTypeType.CONFIG);
        poll.getPollReceiver().addAll(pollReceiver);
        poll.getPollPayload().addAll(pollPayload);

        return poll;
    }

    public PollType createManualPoll() {
        KeyValueType dnid = new KeyValueType();
        dnid.setKey("DNID"); dnid.setValue(String.valueOf(DNID));
        KeyValueType address = new KeyValueType();
        address.setKey("SATELLITE_NUMBER"); address.setValue(String.valueOf(ADDRESS));
        KeyValueType memberNumber = new KeyValueType();
        memberNumber.setKey("MEMBER_NUMBER"); memberNumber.setValue(String.valueOf(MEMBER_NUMBER));

        List<KeyValueType> attributes = new ArrayList<>(Arrays.asList(
                dnid, address, memberNumber));

        PollType poll = new PollType();
        poll.setPollTypeType(PollTypeType.POLL);
        poll.getPollReceiver().addAll(attributes);

        return poll;
    }
}
