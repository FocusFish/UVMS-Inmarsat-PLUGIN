package fish.focus.uvms.plugins.inmarsat.data;

import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;

import java.util.Collections;
import java.util.List;

public class ManualPoll extends InmarsatPoll {

    public ManualPoll(String oceanRegion) {
        super(oceanRegion);
    }

    private int dnid;
    private String address;
    private int memberNumber = 1;


    @Override
    public void setFieldsFromPollRequest(PollType poll) {
        for (KeyValueType element : poll.getPollReceiver()) {
            if (element.getKey().equalsIgnoreCase("DNID")) {
                dnid = Integer.parseInt(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("SATELLITE_NUMBER")) {
                address = element.getValue();
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                memberNumber = Integer.parseInt(element.getValue());
            }
        }
    }

    @Override
    public List<String> asCommand() {
        String start = buildStartIndividualPoll();
        return Collections.singletonList(start);
    }

    /**
     * Usage: poll <Ocean Region>,<P1>,<P2>,<P3>,<P4>,<P5>,<P6>,<P7>,<P8>,<P9>,<P10>,<P11>,<P12>
     * <p>
     * P01 - Poll type (G,I,N,R,C)
     * P02 - int DNID (up to 5 digits)
     * P03 - Response type (D,M,N)
     * P04 - Sub address (0-255)  (0 default)
     * P05 - Address (9 Digits)
     * P06 - Command type (00-11)
     * P07 - Member number(1-255) (1 default)
     * P08 - Start frame (4 digits) (0 default)
     * P09 - Reports per 24 hours (3 digit) (Max 500, default 10)
     * P10 - Acknowledgement (0-1) (0 default)
     * P11 - Spot id (0 default)
     * P12 - MES Serial (empty default)
     */
    private String buildStartIndividualPoll() {
        return String.format("poll %s,I,%s,D,1,%s,0,%s", oceanRegion, String.valueOf(dnid), address, String.valueOf(memberNumber));
    }
}
