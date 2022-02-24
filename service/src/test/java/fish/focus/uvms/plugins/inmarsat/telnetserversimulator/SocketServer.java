package fish.focus.uvms.plugins.inmarsat.telnetserversimulator;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

@Startup
@Singleton
public class SocketServer {

    private Server server = null;


    @PostConstruct
    private void startup() {

        try {


            server = new Server(9090);
            server.registerCommand(new Command("DNID") {
                @Override
                public Response handle(String arguments) throws UnsupportedEncodingException {
                    if (arguments.length() > 0) {
                        DNIDHandler dnidHandler = new DNIDHandler(arguments);
                        if (dnidHandler.verify()) {
                            return dnidHandler.execute();
                        } else {
                            return new Response("DNID request not OK".getBytes("UTF-8"));
                        }
                    } else {
                        return new Response("No arguments passed in DNID command".getBytes("UTF-8"));
                    }
                }

            });
            server.registerCommand(new Command("TEST") {
                @Override
                public Response handle(String arguments) throws UnsupportedEncodingException {
                    if (arguments.length() > 0) {
                        TestHandler  handler = new TestHandler(arguments);
                        if (handler.verify()) {
                            return handler.execute();
                        } else {
                            return new Response("TEST request not OK".getBytes("UTF-8"));
                        }
                    } else {
                        return new Response("No arguments passed in TEST command".getBytes("UTF-8"));
                    }
                }

            });

            server.registerCommand(new Command("DECODE") {
                @Override
                public Response handle(String arguments) throws UnsupportedEncodingException {
                    if (arguments.length() > 0) {
                            return new Response("DECODE request OK".getBytes("UTF-8"));
                    } else {
                        return new Response("No arguments passed in DECODE command".getBytes("UTF-8"));
                    }
                }

            });

            server.registerCommand(new Command("quit") {

                @Override
                public Response handle(String arguments) throws UnsupportedEncodingException {
                    return new Response("bye".getBytes("UTF-8"), false);
                }

            });

            server.start();






        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("hepp");

    }

    @PreDestroy
    private void shutdown() {

        server.stop();

    }




}
