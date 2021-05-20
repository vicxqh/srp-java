package vicxqh.srp.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vicxqh.srp.proto.Protocal.Header;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServiceManager extends Thread{
    private static final Logger Log = LoggerFactory.getLogger(ServiceManager.class);

    private ServerConnection sc;
    private HashMap<String, Service> services;

    public ServiceManager(ServerConnection sc) {
        this.sc = sc;
        this.services = new HashMap<>();
    }

    @Override
    public void run(){
        while (true) {
            try {
                Segment seg = this.sc.take();
                String key = seg.getHeader().getUser() + "<->" + seg.getHeader().getService();
                Service s = this.services.get(key);
                if (s == null) {
                    s = new Service(seg.getHeader());
                    Log.info("starting new service {}", s.key);
                    s.start();
                    this.services.put(key, s);
                }
                s.toService(seg);
            } catch (InterruptedException e) {
                Log.error("service manager was interrupted, exiting");
                return;
            }
        }
    }

    private class Service extends Thread {
        String key;
        Header header;
        BlockingQueue<Segment> send;

        Service(Header header) {
            this.key = header.getUser() + "<->" + header.getService();
            this.header = header;
            this.send = new LinkedBlockingQueue<>(10);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String[] ss = this.header.getService().split(":");
                    Socket socket = new Socket(ss[0], Integer.valueOf(ss[1]));
                    InputStream fromService = socket.getInputStream();
                    OutputStream toService = socket.getOutputStream();

                    ConnectionCondition connectionState = new ConnectionCondition();
                    Thread sender = new Sender(toService, connectionState);
                    Thread receiver = new Receiver(fromService, connectionState);
                    sender.start();
                    receiver.start();
                    InterruptedException ie = null;
                    try {
                        connectionState.waitUntilDropped();
                        sender.interrupt();
                        receiver.interrupt();
                        sender.join();
                        receiver.join();
                    } catch (InterruptedException e) {
                        Log.error("{} service thread interrupted", key);
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void toService(Segment seg) throws InterruptedException {
            this.send.put(seg);
        }

        Header newHeader(int payloadLength) {
            Header h = new Header(Arrays.copyOf(this.header.toBytes(), Header.Size));
            h.setPayloadLength(payloadLength);
            return h;
        }

        class Sender extends Thread {
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
                        try {
                            Segment seg = send.take();
                            this.out.write(seg.getData());
                        } catch (IOException e) {
                            Log.error("{} failed to write to service, {}", key, e);
                            this.connectionState.drop();
                            break;
                        }
                    } catch (InterruptedException e) {
                        Log.warn("{} sender thread was interrupted, exiting...", key);
                        break;
                    }
                }
                Log.info("{} sender thread exited", key);
            }
        }

        class Receiver extends Thread {
            private final int BUF_SIZE = 1024;
            InputStream in;
            ConnectionCondition connectionState;

            public Receiver(InputStream in, ConnectionCondition connectionState) {
                this.in = in;
                this.connectionState = connectionState;
            }

            @Override
            public void run() {
                byte[] buf = new byte[BUF_SIZE];
                while (true) {
                    try {
                        int n = this.in.read(buf);
                        if (n == -1) {
                            this.connectionState.drop();
                            break;
                        }
                        sc.sendToServer(new Segment(newHeader(n), Arrays.copyOf(buf, n)));
                    } catch (IOException e) {
                        Log.error("{} failed to read from service, {}", key, e);
                        this.connectionState.drop();
                        break;
                    }
                }
                Log.info("{} receiver thread exited", key);
            }
        }
    }
}
