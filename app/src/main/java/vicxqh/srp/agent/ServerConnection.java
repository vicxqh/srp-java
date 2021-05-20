package vicxqh.srp.agent;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vicxqh.srp.proto.Protocal.Header;

public class ServerConnection implements Closeable {
    private String server;
    private AgentRegistrationRequest regRequest;

    private final static int BUFFER_SIZE = 10;
    private static final Logger Log = LoggerFactory.getLogger(ServerConnection.class);
    private BlockingQueue<Segment> recv;
    private BlockingQueue<Segment> send;

    public ServerConnection(String server) {
        this(server, AgentRegistrationRequest.DEFAULT);
    }

    public ServerConnection(String server, AgentRegistrationRequest regRequest) {
        this.server = server;
        this.recv = new LinkedBlockingQueue<>(BUFFER_SIZE);
        this.send = new LinkedBlockingQueue<>(BUFFER_SIZE);
        this.regRequest = regRequest;
    }

    public void connect() throws InterruptedException {
        boolean retrying = false;
        int backoff = 1000; // ms
        final int maxBackoff = 10_1000;
        while (true) {
            if (retrying) {
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Log.warn("interrupted");
                    return;
                }
                backoff *= 2;
                if (backoff >= maxBackoff) {
                    backoff = maxBackoff;
                }
            }
            retrying = true;
            try {
                URL url = new URL(String.format("http://%s/api/v1/dataport", this.server));
                Log.info("connecting to {}", url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("accept", "application/json");
                int port = 0;
                try (InputStream responseStream = connection.getInputStream()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                    String body = reader.readLine();
                    Log.info("data port is {}", body);
                    port = Integer.valueOf(body);
                }
                if (port == 0) {
                    Log.error("failed to get data port, retrying...");
                    continue;
                }

                Socket socket = new Socket(this.server.substring(0, this.server.indexOf(":")), port);
                InputStream fromServer = socket.getInputStream();
                OutputStream toServer = socket.getOutputStream();

                // handshake
                ObjectMapper mapper = new ObjectMapper();
                byte[] regData = mapper.writeValueAsBytes(this.regRequest);
                Log.debug("request {}", regData);
                toServer.write(regData);
                byte[] buff = new byte[1024];
                int n = fromServer.read(buff);
                AgentRegistrationResponse rsp = mapper.readValue(buff, 0, n, AgentRegistrationResponse.class);
                Log.debug("response {}", rsp);
                if (!rsp.succeeded) {
                    continue;
                }
                Log.info("connected to data server");

                ConnectionCondition connectionState = new ConnectionCondition();
                Thread sender = new Sender(toServer, connectionState);
                Thread receiver = new Receiver(fromServer, connectionState);
                Log.info("starting new sender");
                sender.start();
                Log.info("starting new receiver");
                receiver.start();
                InterruptedException ie = null;
                try {
                    connectionState.waitUntilDropped();
                } catch (InterruptedException e) {
                    Log.error("main thread interrupted");
                    ie = e;
                }
                sender.interrupt();
                receiver.interrupt();
                sender.join();
                receiver.join();
                if (ie != null) {
                    throw ie;
                }
            } catch (IOException e) {
                Log.warn("failed to reconnect, {}", e);
            }
        }
    }

    public static void main(String[] args) {
        ServerConnection s = new ServerConnection("65.52.171.89:8010");
        try {
            s.connect();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendToServer(Segment seg) {
        try {
            this.send.put(seg);
        } catch (InterruptedException e) {
            Log.warn("failed to put into send queue, {}", e);
        }
    }

    public Segment take() throws InterruptedException {
        return this.recv.take();
    }

    @Override
    public void close() throws IOException {

    }

    private class Sender extends Thread {
        OutputStream out;
        ConnectionCondition connectionState;

        public Sender(OutputStream out, ConnectionCondition connectionState) {
            this.out = out;
            this.connectionState = connectionState;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Segment seg = send.take();
                    try {
                        this.out.write(seg.getHeader().toBytes());
                        this.out.write(seg.getData());
                    } catch (IOException e) {
                        Log.error("failed to write to server, {}", e);
                        this.connectionState.drop();
                        break;
                    }
                } catch (InterruptedException e) {
                    Log.warn("sender thread was interrupted, exiting...");
                    break;
                }
            }
            Log.info("sender thread exited");
        }
    }

    private class Receiver extends Thread {
        InputStream in;
        ConnectionCondition connectionState;

        public Receiver(InputStream in, ConnectionCondition connectionState) {
            this.in = in;
            this.connectionState = connectionState;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    try {
                        byte[] headerData = new byte[Header.Size];
                        this.readFull(headerData);
                        Header header = new Header(headerData);
                        byte[] body = new byte[header.getPayloadLength()];
                        this.readFull(body);
                        recv.put(new Segment(header, body));
                    } catch (IOException e) {
                        Log.error("failed to read from server, {}", e);
                        this.connectionState.drop();
                        break;
                    }
                } catch (InterruptedException e) {
                    Log.warn("receiver thread was interrupted, exiting...");
                    break;
                }
            }
            Log.info("receiver thread exited");
        }

        public void readFull(byte[] buf) throws IOException {
            int read = 0;
            while (read < buf.length) {
                int more = this.in.read(buf, read, buf.length - read);
                read += more;
            }
        }
    }
}
