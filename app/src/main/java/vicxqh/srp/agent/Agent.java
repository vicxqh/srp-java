package vicxqh.srp.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Agent implements Runnable{
    private static final Logger Log = LoggerFactory.getLogger(Agent.class);

    private ServerConnection sc;
    private ServiceManager sm;

    public Agent(String server) {
        this(server, null, null);
    }

    public Agent(String server, String id, String description) {
        if (id == null) {
            this.sc = new ServerConnection(server);
        } else {
            this.sc = new ServerConnection(server, new AgentRegistrationRequest(id, description));
        }
        this.sm = new ServiceManager(this.sc);
    }

    @Override
    public void run() {
        this.sm.start();
        try {
            this.sc.connect();
        } catch (InterruptedException e) {
            Log.info("exiting...");
        }
    }
}
