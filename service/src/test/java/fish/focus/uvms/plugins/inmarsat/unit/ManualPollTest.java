package fish.focus.uvms.plugins.inmarsat.unit;

import fish.focus.schema.exchange.plugin.types.v1.PollType;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPoll;
import fish.focus.uvms.plugins.inmarsat.data.ManualPoll;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ManualPollTest {

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
    public void createAndValidateManualPollCommand() {
        PollType pollType = pollHelper.createManualPoll();
        InmarsatPoll poll = new ManualPoll(pollHelper.OCEAN_REGION);
        poll.setFieldsFromPollRequest(pollType);

        List<String> commandList = poll.asCommand();
        String expectedPollCommand = "poll 1,I,12345,D,1,412345678,0,101";

        Assert.assertEquals(1, commandList.size());
        Assert.assertEquals(expectedPollCommand, commandList.get(0));
    }
}
