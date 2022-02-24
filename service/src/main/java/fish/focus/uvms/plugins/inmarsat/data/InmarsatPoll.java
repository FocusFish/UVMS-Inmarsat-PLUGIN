package fish.focus.uvms.plugins.inmarsat.data;

import fish.focus.schema.exchange.plugin.types.v1.PollType;

import java.util.List;

public abstract class InmarsatPoll {

    final String oceanRegion;

    InmarsatPoll(String oceanRegion) {
        this.oceanRegion = oceanRegion;
    }

    public abstract void setFieldsFromPollRequest(PollType poll);
    public abstract List<String> asCommand();
}
