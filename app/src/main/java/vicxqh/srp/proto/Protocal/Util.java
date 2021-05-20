package vicxqh.srp.proto.Protocal;

public class Util {
    /**
     * Converts an uint32 to a byte array in little endian format.
     * @param num treated as uint32
     * @return byte[4]
     */
    public static byte[] intToBytes(int num) {
        byte[] ret = new byte[]{(byte)(num>>24), (byte)(num>>16), (byte)(num>>8), (byte)(num)};
        return ret;
    }

    public static int bytesToInt(byte[] bytes){
        int ret = 0;
        for (int i = 0; i < 4 && i < bytes.length; i ++) {
            ret <<= 8;
            ret |= 0xFF & (int)bytes[i];
        }
        return ret;
    }
}
