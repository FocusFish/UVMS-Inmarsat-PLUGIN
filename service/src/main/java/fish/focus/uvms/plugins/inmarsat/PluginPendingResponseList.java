package fish.focus.uvms.plugins.inmarsat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.uvms.plugins.inmarsat.data.InmarsatPendingResponse;
import javax.ejb.LocalBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@LocalBean
public class PluginPendingResponseList {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginPendingResponseList.class);

    private static final long PENDING_RESPONSE_TTL_MINUTES = 8L;

    private ArrayList<InmarsatPendingResponse> pending = new ArrayList<>();

    public void addPendingPollResponse(InmarsatPendingResponse resp) {
        if (pending != null) {
            pending.add(resp);
            LOGGER.debug("Pending response added");
        }
    }

    public boolean removePendingPollResponse(InmarsatPendingResponse resp) {
        if (pending != null) {
            LOGGER.debug("Trying to remove pending poll response");
            return pending.remove(resp);
        }
        return false;
    }

    public List<InmarsatPendingResponse> getPendingPollResponses() {
        return new ArrayList<>(pending);
    }

    public InmarsatPendingResponse containsPollFor(String dnid, String memberId) {
        // Remove stale items
        pending.removeIf(p -> Duration.between(p.getCreatedAt(), Instant.now())
                                .toMinutes() > PENDING_RESPONSE_TTL_MINUTES);
        for (InmarsatPendingResponse element : pending) {
            if (element.getDnId().equalsIgnoreCase(dnid) && element.getMembId().equalsIgnoreCase(memberId)) {
                return element;
            }
        }
        return null;
    }

    public String dump() {
        String ret = "";
        for (InmarsatPendingResponse element : pending) {
            ret += element.toString();
            ret += System.lineSeparator();
        }
        return ret;
    }
}
