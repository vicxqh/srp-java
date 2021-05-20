package vicxqh.srp.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class AgentRegistrationRequest {
    public String id;
    public String description;

    public static final AgentRegistrationRequest DEFAULT;
    static {
        String hostname = null;
        try {
             hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (hostname == null) {
            hostname = UUID.randomUUID().toString();
        }
        DEFAULT = new AgentRegistrationRequest(hostname, hostname);
    }
    public AgentRegistrationRequest(String id, String description) {
        this.id = id;
        this.description = description;
    }
}
