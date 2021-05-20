package vicxqh.srp.agent;

import vicxqh.srp.proto.Protocal.Header;

public class Segment {
    private Header header;
    private byte[] data;

    public Segment(Header header, byte[] data) {
        this.header = header;
        this.data = data;
    }

    public Header getHeader() {
        return header;
    }

    public byte[] getData() {
        return data;
    }
}
