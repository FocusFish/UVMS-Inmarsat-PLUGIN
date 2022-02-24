package fish.focus.uvms.plugins.inmarsat;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import fish.focus.uvms.plugins.inmarsat.PluginPendingResponseList;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPendingResponse;

public class PluginPendingResponseListTest {

    @Test
    public void removeItemTest() {
        PluginPendingResponseList responseList = new PluginPendingResponseList();
        InmarsatPendingResponse item = new InmarsatPendingResponse();
        item.setCreatedAt(Instant.now());
        item.setDnId("123");
        item.setMembId("45");
        responseList.addPendingPollResponse(item);
        responseList.containsPollFor("", "");
        assertThat(responseList.getPendingPollResponses().size(), is(1));
    }

    @Test
    public void removeStaleItemTest() {
        PluginPendingResponseList responseList = new PluginPendingResponseList();
        InmarsatPendingResponse item = new InmarsatPendingResponse();
        item.setCreatedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        responseList.addPendingPollResponse(item);
        responseList.containsPollFor("", "");
        assertThat(responseList.getPendingPollResponses().size(), is(0));
    }
}
