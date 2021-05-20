package vicxqh.srp.agent;


public class ConnectionCondition{
    private boolean dropped = false;

    public synchronized void waitUntilDropped() throws InterruptedException {
        while (true) {
            if (dropped) {
                return;
            }
            wait();
        }
    }

    public synchronized void drop(){
        this.dropped = true;
        notifyAll();
    }
}
