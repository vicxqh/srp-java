package vicxqh.srp.proto.Protocal;

import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest extends TestCase {

    @Test
    public void testBytesIntConversion() {
        int[] cases =  new int[]{
                0, 1, 65535,192_168_1_1,
        };
        for (int i : cases) {
            assertEquals(i, Util.bytesToInt(Util.intToBytes(i)));
        }
    }

    @Test
    public void testBytesToInt(){
        // test port number conversion
        int[] cases = new int[]{0, 80, 65532};
        for (int i : cases) {
            byte[] tmp = Util.intToBytes(i);
            byte[] port = new byte[]{tmp[2], tmp[3]};
            assertEquals(i, Util.bytesToInt(port));
        }
    }
}