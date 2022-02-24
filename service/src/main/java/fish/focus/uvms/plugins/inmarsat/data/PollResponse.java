package fish.focus.uvms.plugins.inmarsat.data;

public class PollResponse {

    String reference;
    String message;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
