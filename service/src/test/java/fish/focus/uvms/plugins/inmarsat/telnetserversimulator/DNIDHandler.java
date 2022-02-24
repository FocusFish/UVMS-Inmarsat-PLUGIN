package fish.focus.uvms.plugins.inmarsat.telnetserversimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DNIDHandler {

	public static final String END = "\r\n";
	private static final Logger LOGGER = LoggerFactory.getLogger(DNIDHandler.class);

	private String dnid;
	private String area;
	String dnidRoot = null;
	String arguments;

	public DNIDHandler(String arguments) {
		this.arguments = arguments;
	}

	private boolean parseArguments() {

		String parts[] = arguments.split(" ");
		if (parts.length < 2) {
			return false;
		}
		if (parts.length >= 1) {
			dnid = parts[0];
		}
		if (parts.length >= 2) {
			area = parts[1];
		} else {
			area = "1";

		}
		return true;
	}

	public boolean verify() {

		if (!parseArguments()) {
			LOGGER.error("arguments not ok . Check your call");
			return false;
		}

		return true;
	}

	public Response execute() {

			return new Response("was in execute >".getBytes());


	}
}
