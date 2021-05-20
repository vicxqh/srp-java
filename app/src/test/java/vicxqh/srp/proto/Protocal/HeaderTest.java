package vicxqh.srp.proto.Protocal;

import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class HeaderTest extends TestCase {
    @Test
    public void testHeader() {
        Header header = new Header("123.1.2.3", 65520, "192.168.1.2", 8080);
        assertEquals("123.1.2.3:65520", header.getUser());
        assertEquals("192.168.1.2:8080", header.getService());
        assertEquals(0, header.getPayloadLength());
        header.setPayloadLength(12345678);
        assertEquals(12345678, header.getPayloadLength());
    }
}