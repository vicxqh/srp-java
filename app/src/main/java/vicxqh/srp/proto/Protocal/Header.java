package vicxqh.srp.proto.Protocal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Header
 *
 *  0                                 31
 *  +---------------------------------+  ---+
 *  |             user ip             |     |
 *  +---------------------------------+     |
 *  |            service ip           |     |
 *  +----------------+----------------+     |-- 16 bytes
 *  |    user port   |  service port  |     |
 *  +---------------------------------+     |
 *  |          payload length         |     |
 *  +----------------+----------------+  ---+
 *  |                                 |
 *  |            payload              |
 *  |                                 |
 *  +---------------------------------+
 *
 */
public class Header {
    private static final Logger Log = LoggerFactory.getLogger(Header.class);
    public static final int Size = 16;

    private byte[] data;

    public Header(byte[] data) {
        this.data = data;
    }

    public Header (String userAddr, int userPort,  String serviceAddr, int servicePort) {
        this(new InetSocketAddress(userAddr, userPort), new InetSocketAddress(serviceAddr, servicePort), 0);
    }

    public Header(InetSocketAddress user, InetSocketAddress service, int payloadLength){
        byte[] userIP = user.getAddress().getAddress();
        byte[] serviceIP = service.getAddress().getAddress();
        byte[] userPort = Util.intToBytes(user.getPort());
        byte[] servicePort = Util.intToBytes(service.getPort());

        this.data = new byte[]{
                userIP[0], userIP[1], userIP[2], userIP[3],
                serviceIP[0], serviceIP[1], serviceIP[2], serviceIP[3],
                userPort[2], userPort[3], servicePort[2], servicePort[3],
                0,0,0,0
        };
        this.setPayloadLength(payloadLength);
    }

    public void setPayloadLength(int length) {
        byte[] bytes = Util.intToBytes(length);
        this.data[12] = bytes[0];
        this.data[13] = bytes[1];
        this.data[14] = bytes[2];
        this.data[15] = bytes[3];
    }

    public int getPayloadLength() {
        return Util.bytesToInt(Arrays.copyOfRange(this.data, 12, 16));
    }

    public String getUser()  {
        InetSocketAddress addr = null;
        try {
            addr = new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(this.data,0, 4)),
                    Util.bytesToInt(Arrays.copyOfRange(this.data, 8, 10)));
        } catch (UnknownHostException e) {
            Log.warn("failed to parse user address, {}", e);
        }
        return addr.toString().substring(1);
    }

    public String getService() {
        InetSocketAddress addr = null;
        try {
            addr = new InetSocketAddress(InetAddress.getByAddress(Arrays.copyOfRange(this.data,4, 8)),
                    Util.bytesToInt(Arrays.copyOfRange(this.data, 10, 12)));
        } catch (UnknownHostException e) {
            Log.warn("failed to parse service address, {}", e);
        }
        return addr.toString().substring(1);
    }

    public byte[] toBytes() {
        return this.data;
    }
}
