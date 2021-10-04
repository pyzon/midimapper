package kristofkallo.midimapper;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.awt.*;
import java.util.Arrays;

import static kristofkallo.midimapper.App.APP_NAME;

public class M400Receiver implements Receiver {
    private static final byte SYS_EX_STATUS_BYTE = (byte) 0xf0;
    private static final byte EOX = (byte) 0xf7; // End of System Exclusive message
    private static final byte MANUFACTURER_ID = 0x41;
    private static final byte DEVICE_ID = 0x00;
    private static final byte[] MODEL_ID = {0x00, 0x00, 0x24};
    private static final byte DATA_SET_COMMAND_ID = 0x12;
    private static class MessageCategory {
        static final byte INPUT_CHANNEL = 0x03;
        static final byte MAIN_CHANNEL = 0x06;
        // TODO: Others maybe
    }
    private static class MessageType {
        static final byte[] PAN = {0x00, 0x10};

        static final byte[] FILTER_ATT = {0x00, 0x22};

        static final byte[] EQ_1_FREQ = {0x00, 0x55};
        static final byte[] EQ_2_FREQ = {0x00, 0x5a};
        static final byte[] EQ_2_Q = {0x00, 0x5d};

    }

    private final Receiver receiver;

    public M400Receiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        // Sanity check
        if (receiver == null) {
            Main.app.getTrayMenu().getTrayIcon().displayMessage(APP_NAME, "Receiver is null. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            return;
        }
        byte[] msg = message.getMessage();
        // SysEx status byte f0 which is -16
        if (msg[0] == SYS_EX_STATUS_BYTE &&
            msg[1] == MANUFACTURER_ID &&
            msg[2] == DEVICE_ID &&
            Arrays.equals(Arrays.copyOfRange(msg, 3, 6), MODEL_ID) &&
            msg[6] == DATA_SET_COMMAND_ID) {
            try {
                int mixerChannel = 127;
                switch (msg[7]) {
                    case MessageCategory.INPUT_CHANNEL:
                        mixerChannel = msg[8];
                        break;
                    case MessageCategory.MAIN_CHANNEL:
                        mixerChannel = 72;
                        break;
                }
                // Common parameters amongst input, main, and aux channels
                if (msg[7] == MessageCategory.INPUT_CHANNEL ||
                        msg[7] == MessageCategory.MAIN_CHANNEL) {
                    byte param = 127;
                    int value = 0;
                    byte[] messageType = Arrays.copyOfRange(msg, 9, 11);
                    if (Arrays.equals(messageType, MessageType.EQ_1_FREQ)) {
                        value = logToLin(squash(Arrays.copyOfRange(msg, 11, 14)), 20, 20000, 16384, 10, 30000);
                        param = 0;
                    } /*else if (Arrays.equals(messageType, MessageType.EQ_2_FREQ)) {
                        value = frequencyToSliderValue(squash(Arrays.copyOfRange(msg, 11, 14)));
                        param = 1;
                    } else if (Arrays.equals(messageType, MessageType.EQ_2_Q)) {
                        value = linearToSliderValue(squash(Arrays.copyOfRange(msg, 11, 13)), 0, 1200);
                        param = 2;
                    }*/
                    byte[] valueParts = split(value);

                    ShortMessage outMsg = new ShortMessage();
//                    outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, param, value);
                    outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 99, 0);
                    receiver.send(outMsg, timeStamp);
                    outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 98, 0);
                    receiver.send(outMsg, timeStamp);
                    outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 6, valueParts[0]);
                    receiver.send(outMsg, timeStamp);
                    outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 38, valueParts[1]);
                    receiver.send(outMsg, timeStamp);
                }
            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }
//        if (msg[0] == -0x10) {
//            // F0 41 00 00 00 24 12 03 channel 00 10 value1 value0 F7
//            if (msg[1] == 0x41 && msg[2] == 0x00 && msg[3] == 0x00 && msg[4] == 0x00 & msg[5] == 0x24) {
//                if (msg[6] == 0x12) {
//                    if (msg[7] == 0x03) {
//                        byte mixerChannel = (byte) (msg[8]); // TODO: pay attention to overflow
//                        if (msg[9] == 0x00 && msg[10] == 0x10) {
//                            byte value = (byte) ((msg[11] * 0x100 + msg[12]) / 0x100); // TODO: overflow
//                            try {
//                                ShortMessage outMsg = new ShortMessage();
//                                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 1, mixerChannel, value);
//                                //System.out.println(outReceiver);
//                                if (outReceiver != null)
//                                    outReceiver.send(outMsg, timeStamp);
//                                //System.out.println(timeStamp);
//
//                            } catch (InvalidMidiDataException e) {
//                                e.printStackTrace();
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
    }

    @Override
    public void close() {

    }

    /**
     * Interprets an array of 7-bit bytes as a signed number. The first byte is the MSB.
     * The number follows the two's complement representation.
     * @param bytes Array of 7-bit bytes, meaning that the first bit is 0 and is
     *              ignored during the interpretation.
     * @return The squashed number as an int.
     */
    private int squash(byte[] bytes) {
        int result = 0;
        for(int i = 0; i < bytes.length; i++) {
            result += bytes[i] << ((bytes.length - i - 1) * 7);
        }
        // negative
        if (bytes[0] >= 64) {
            int mask = -1;
            mask = mask << (bytes.length * 7);
            result = result | mask;
        }
        return result;
    }

    /**
     * Splits a 14 digit unsigned integer into two 7-bit bytes.
     * @param number The number to split.
     * @return Array of two bytes, the first one being the MSB.
     */
    private byte[] split(int number) {
        System.out.println(number);
        if (number < 0 || 16383 < number) {
            throw new IllegalArgumentException("expected unsigned 14-bit number");
        }
        return new byte[]{(byte)(number >> 7), (byte)(number & 127)};
    }
    private int logToLin(double source, double sourceMin, double sourceMax, double destRes, double destMin, double destMax) {
        if (source < sourceMin) {
            source = sourceMin;
        }
        if (source > sourceMax) {
            source = sourceMax;
        }
        return (int) Math.round(destRes * Math.log(source/destMin) / Math.log(destMax/destMin));
    }
    private byte linearToSliderValue(double level, double min, double max) {
        if (level < min || level > max)
            throw new IllegalArgumentException("level is out of range");
        return (byte) Math.round(127 * (level - min) / (max - min));
    }
}
