package vicxqh.srp.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentRegistrationResponse {
    @JsonProperty("Succeeded")
    public boolean succeeded;

    @JsonProperty("Message")
    public String message;
}
