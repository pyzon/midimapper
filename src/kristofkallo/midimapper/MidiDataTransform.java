package kristofkallo.midimapper;

/**
 * Encapsulates a integer. Has methods that transform from and into
 * a 7-bit byte array.
 */
public interface MidiDataTransform {
    /**
     * Interprets an array of 7-bit bytes as an integer.
     * The first byte in the array is the MSB.
     * If signed, the number follows the two's complement representation.
     *
     * @param bytes Array of 7-bit bytes, which means that the first bit of the
     *              bytes must be a 0.
     * @param isSigned Type of the byte array representation.
     * @return The converted number.
     */
    static int fromByteArray(byte[] bytes, boolean isSigned) {
        if (isSigned) {
            return fromByteArraySigned(bytes);
        }
        return fromByteArrayUnsigned(bytes);
    }

    /**
     * Interprets an array of 7-bit bytes as a signed integer.
     * The first byte in the array is the MSB.
     * The number follows the two's complement representation.
     *
     * @param bytes Array of 7-bit bytes, which means that the first bit of the
     *              bytes must be a 0.
     * @return The converted number.
     */
    static int fromByteArraySigned(byte[] bytes) {
        int result = fromByteArrayUnsigned(bytes);
        // negative
        if (bytes[0] >= 64) {
            int mask = -1;
            mask = mask << (bytes.length * 7);
            result = result | mask;
        }
        return result;
    }

    /**
     * Interprets an array of 7-bit bytes as an unsigned integer.
     * The first byte in the array is the MSB.
     *
     * @param bytes Array of 7-bit bytes, which means that the first bit of the
     *              bytes must be a 0.
     * @return The converted number.
     */
    static int fromByteArrayUnsigned(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < bytes.length; i++) {
            result += bytes[i] << ((bytes.length - i - 1) * 7);
        }
        return result;
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
    static byte[] toByteArray(int data, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) ((data >> ((length - i - 1) * 7)) & 127);
        }
        return result;
    }
}
