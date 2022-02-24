package fish.focus.uvms.plugins.inmarsat.data;

import fish.focus.schema.exchange.common.v1.KeyValueType;
import fish.focus.schema.exchange.plugin.types.v1.PollType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ConfigPoll extends InmarsatPoll {

    public ConfigPoll(String oceanRegion) {
        super(oceanRegion);
    }

    private int dnid;
    private long address;
    private int memberNumber = 1;
    private int startFrame = 0;
    private int frequency = 10;
    private int gracePeriod;
    private int inPortGrace;
    private int startFrameHour;
    private int startFrameMinute;

    @Override
    public void setFieldsFromPollRequest(PollType poll) {
        setFieldsFromPollReceiver(poll.getPollReceiver());
        setFieldsFromPollPayload(poll.getPollPayload());
    }

    private void setFieldsFromPollReceiver(List<KeyValueType> receiver) {
        for (KeyValueType element : receiver) {
            if (element.getKey().equalsIgnoreCase("DNID")) {
                dnid = Integer.parseInt(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("SATELLITE_NUMBER")) {
                address = Long.parseLong(element.getValue());
            } else if (element.getKey().equalsIgnoreCase("MEMBER_NUMBER")) {
                memberNumber = Integer.parseInt(element.getValue());
            }
        }
    }

    private void setFieldsFromPollPayload(List<KeyValueType> payload) {
        for (KeyValueType element : payload) {
            if (element.getKey().equalsIgnoreCase("REPORT_FREQUENCY")) {
                int seconds = Integer.parseInt(element.getValue());
                frequency = 24 * 60 * 60 / seconds;
                if (startFrame == 0) {
                    startFrame = getDefaultStartFrame();
                }
            } else if (element.getKey().equalsIgnoreCase("GRACE_PERIOD")) {
                int seconds = Integer.parseInt(element.getValue());
                secondsToStartFrame(seconds);
            } else if (element.getKey().equalsIgnoreCase("IN_PORT_GRACE")) {
                inPortGrace = Integer.parseInt(element.getValue());
            }
        }
    }

    private void secondsToStartFrame(int seconds) {
        Instant instant = Instant.ofEpochSecond(seconds);
        startFrameHour = instant.atZone(ZoneOffset.UTC).getHour();
        startFrameMinute = instant.atZone(ZoneOffset.UTC).getMinute();
        startFrame = calcStartFrame(startFrameHour, startFrameMinute);
    }

    /**
     * (((hour * 60) + minute) * 60) / 8.64 = startFrame number.
     * Night and Day (24 hours) are divided in 10000 frames equal to 8,64 second.
     * Example: 24 * 60 * 60 / 10000 = 8,64
     * The "24" at the end indicate that the terminals shall send one report every hour.
     * Example: 13:28 o'clock will be equal to 5611
     *
     * @param hour   Hours of the day (24 hours format)
     * @param minute Minutes of the hour
     * @return startFrame
     */

    private int calcStartFrame(int hour, int minute) {
        if ((hour < 0) || (hour > 24)) {
            throw new IllegalArgumentException("Hour must be between 0 and 24. Was " + hour);
        }
        if ((minute < 0) || (minute > 60)) {
            throw new IllegalArgumentException("Minute must be between 0 and 60. Was " + minute);
        }
        if (hour != 0 || minute > 0) {
            return (int) ((((hour * 60) + minute) * 60) / 8.64);
        }

        return getDefaultStartFrame();
    }

    private int getDefaultStartFrame() {
        Instant instant = Instant.now().plus(10, ChronoUnit.MINUTES);
        int startHour = instant.atZone(ZoneOffset.UTC).getHour();
        int startMinute = instant.atZone(ZoneOffset.UTC).getMinute();
        return (int) ((((startHour * 60) + startMinute) * 60) / 8.64);
    }

    @Override
    public List<String> asCommand() {
        String stop = buildStopIndividualPoll();
        String config = buildConfigIndividualPoll();
        String start = buildStartIndividualPoll();
        List<String> commandList = new ArrayList<>(3);
        commandList.add(0, stop);
        commandList.add(1, config);
        commandList.add(2, start);
        return commandList;
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
    private String buildStopIndividualPoll() {
        return String.format("poll %s,I,%s,N,1,%s,6,%s", oceanRegion, dnid, address, memberNumber);
    }

    private String buildConfigIndividualPoll() {
        return String.format("poll %s,I,%s,N,1,%s,4,%s,%s,%s", oceanRegion, dnid, address, memberNumber, startFrame, frequency);
    }

    private String buildStartIndividualPoll() {
        return String.format("poll %s,I,%s,N,1,%s,5,%s", oceanRegion, dnid, address, memberNumber);
    }

}
