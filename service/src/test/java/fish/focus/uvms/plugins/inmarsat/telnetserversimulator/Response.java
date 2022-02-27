package fish.focus.uvms.plugins.inmarsat.telnetserversimulator;

import java.io.UnsupportedEncodingException;

public class Response {
	private byte[] response = null;
	private boolean keepalive = true;

	public boolean keepalive() {
		return keepalive;
	}

	public void keepalive(boolean keepalive) {
		this.keepalive = keepalive;
	}

	public Response(byte[] data, boolean keepalive) {
		this.response = data;
		this.keepalive = keepalive;
	}

	public Response(byte[] data) {
		this.response = data;
	}

	public byte[] getBytes() {
		if (response != null) {
			return response;
		} else {
			return new byte[0];
		}
	}

	public Response() {
		// TODO Auto-generated constructor stub
	}

	public String toString() {
		try {
			return new String(getBytes(), "utf-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	public void set(byte[] response) {
		this.response = response;
	}

}