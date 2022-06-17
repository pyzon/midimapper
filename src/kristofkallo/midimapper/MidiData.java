package kristofkallo.midimapper;

/**
 * Encapsulates a integer. Has methods that transform from and into
 * a 7-bit byte array.
 */
public class MidiData {
    private final int data;

    public MidiData(int data) {
        this.data = data;
    }

    public int getData() {
        return data;
    }

    /**
     * Interprets an array of 7-bit bytes as a signed integer.
     * The first byte in the array is the MSB.
     * The number follows the two's complement representation.
     *
     * @param bytes Array of 7-bit bytes, which means that the first bit of the
     *              bytes must be a 0.
     * @return The created object that holds the converted number.
     */
    public static MidiData fromByteArraySigned(byte[] bytes) {
        MidiData unsigned = fromByteArrayUnsigned(bytes);
        int result = unsigned.data;
        // negative
        if (bytes[0] >= 64) {
            int mask = -1;
            mask = mask << (bytes.length * 7);
            result = result | mask;
        }
        return new MidiData(result);
    }

    /**
     * Interprets an array of 7-bit bytes as an unsigned integer.
     * The first byte in the array is the MSB.
     *
     * @param bytes Array of 7-bit bytes, which means that the first bit of the
     *              bytes must be a 0.
     * @return The created object that holds the converted number.
     */
    public static MidiData fromByteArrayUnsigned(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result += bytes[i] << ((bytes.length - i - 1) * 7);
        }
        return new MidiData(result);
    }

    /**
     * Splits the data into an array of 7-bit bytes.
     * Please note that overflow is not checked because it requires the
     * information whether the representation should be signed or unsigned.
     * Any overflown bits are lost.
     *
     *
     * @param length The target length of the byte array.
     * @return Array of two bytes, the first one being the MSB.
     */
    public byte[] toByteArray(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) ((data >> ((length - i - 1) * 7)) & 127);
        }
        return result;
    }
}
