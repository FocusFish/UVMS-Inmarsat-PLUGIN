package fish.focus.uvms.plugins.inmarsat.telnetserversimulator;

public class POLLHandler {

	private String arguments;

	public POLLHandler(String arguments) {
		this.arguments = arguments;
	}

	public boolean verify() {

		return true;
	}

	public Response execute() {
		return new Response((arguments + " POLL").getBytes());
	}

}
