package fish.focus.uvms.plugins.inmarsat.telnetserversimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	private int port;
	private InetAddress bind = null;
	private ServerSocket socket = null;
	private int timeout = 0;
	private int backlog = 0;

	private static Thread serverThread = null;
	private static boolean loop = true;

	public int getBacklog() {
		return backlog;
	}

	public void setBacklog(int backlog) {
		this.backlog = backlog;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	private HashMap<String, Command> commands = new HashMap<String, Command>();

	public Server(int port, InetAddress bind) throws IOException {
		this.bind = bind;
		this.port = port;
	}

	public Server(int port) throws IOException {
		this.port = port;
	}

	public void registerCommand(String command, Command handler) {
		commands.put(command.toLowerCase(), handler);
	}

	public void registerCommand(Command handler) {
		commands.put(handler.getName().toLowerCase(), handler);
	}

	public HashMap<String, Command> getCommands() {
		return commands;
	}

	// @Override
	public void run() {

		try {
			socket = new ServerSocket(port, backlog, bind);
			socket.setSoTimeout(getTimeout());
		} catch (IOException e) {
			LOGGER.error("Could not get a server socket. Server will not start");
			LOGGER.error(e.toString(), e);
			stop();
		}

		while (loop) {
			try {
				Socket clientSocket = socket.accept();
				Thread jobThread = new Thread(new Channel(clientSocket, this));
				jobThread.start();
			} catch (IOException e) {
				LOGGER.error(e.toString(), e);
			} finally {
			}
		}

		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}

	public void start() {
		serverThread = new Thread(this);
		serverThread.start();
	}

	public void stop() {
		loop = false;
	}


}



