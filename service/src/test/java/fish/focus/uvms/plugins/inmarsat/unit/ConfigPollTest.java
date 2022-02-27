package fish.focus.uvms.plugins.inmarsat.unit;

import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.uvms.plugins.inmarsat.data.ConfigPoll;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPoll;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ConfigPollTest {

    private PollHelper pollHelper;

    @Before
    public void setup() {
        pollHelper = new PollHelper();
    }

    @After
    public void teardown() {
        pollHelper = null;
    }

    @Test
    public void createAndValidateConfigPollCommand() {
        PollType pollType = pollHelper.createConfigPoll();
        InmarsatPoll poll = new ConfigPoll(pollHelper.OCEAN_REGION);
        poll.setFieldsFromPollRequest(pollType);

        String expectedStopCommand = "poll 1,I,12345,N,1,412345678,6,101";
        String expectedConfigCommand = "poll 1,I,12345,N,1,412345678,4,101,5625,12";
        String expectedStartCommand = "poll 1,I,12345,N,1,412345678,5,101";

        List<String> commandList = poll.asCommand();

        Assert.assertEquals(expectedStopCommand, commandList.get(0));
        Assert.assertEquals(expectedConfigCommand, commandList.get(1));
        Assert.assertEquals(expectedStartCommand, commandList.get(2));
    }
}
