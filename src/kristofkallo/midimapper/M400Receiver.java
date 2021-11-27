package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

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

    private final Receiver receiver;
    private final MidiMap midiMap;

    public M400Receiver(Receiver receiver, MidiMap midiMap) {
        this.receiver = receiver;
        this.midiMap = midiMap;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        // Sanity check
        if (receiver == null) {
            Main.app.getTrayMenu().getTrayIcon().displayMessage(APP_NAME, "Receiver is null. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
            return;
        }
        if (midiMap == null) {
            Main.app.getTrayMenu().getTrayIcon().displayMessage(APP_NAME, "MidiMap is null. (This shouldn't happen.)", TrayIcon.MessageType.ERROR);
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
                Channel channel = midiMap.getChannelByAddress(msg[7], msg[8]);
                if (channel == null) {
                    // Channel is not in the map file, i.e. unused
                    return;
                }

                Parameter param = channel.getParameterByAddress(msg[9], msg[10]);
                if (param == null) {
                    // Parameter is not in the map file, i.e. unused
                    return;
                }
                byte[] data = Arrays.copyOfRange(msg, 11, 11 + param.getBytes());
                int value = param.mapConsoleToDAW(squash(data, param.isSigned()));

                byte[] valueParts = split(value);

                ShortMessage outMsg = new ShortMessage();
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 99, channel.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 98, param.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 6, valueParts[0]);
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 38, valueParts[1]);
                receiver.send(outMsg, timeStamp);

            } catch (InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {

    }

    /**
     * Interprets an array of 7-bit bytes as a signed or unsigned number.
     * The first byte is the MSB.
     * The number follows the two's complement representation.
     * @param bytes Array of 7-bit bytes, meaning that the first bit is 0 and is
     *              ignored during the interpretation.
     * @return The squashed number as an int.
     */
    private int squash(byte[] bytes, boolean signed) {
        int result = 0;
        for(int i = 0; i < bytes.length; i++) {
            result += bytes[i] << ((bytes.length - i - 1) * 7);
        }
        // negative
        if (signed && bytes[0] >= 64) {
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
        if (number < 0 || 16383 < number) {
            throw new IllegalArgumentException("expected unsigned 14-bit number");
        }
        return new byte[]{(byte)(number >> 7), (byte)(number & 127)};
    }

//    private double clamp(double number, double min, double max) {
//        // handle inverted intervals
//        if (max < min) {
//            double tmp = min;
//            min = max;
//            max = tmp;
//        }
//        if (number < min) {
//            number = min;
//        }
//        if (number > max) {
//            number = max;
//        }
//        return number;
//    }

//    private int mapRange(double source) {
//        // multiply by (-1) because the scale is inverted in the fabfilter plugin
//        source = clamp(-source / 10, 0, 100);
//        double res = midiMap.getRangeScaleValue(source);
//        return (int) clamp(res, 0, 16383);
//    }
//    private int mapRel(double source) {
//        source = clamp(source, 0, 5000);
//        double y;
//        if (source < 2500) {
//            double x = source / 2500;
//            y = 0.9 * Math.pow(x, 0.3333333333333);
//        } else {
//            y = 0.9 + (source - 2500) / 2500 * 0.1;
//        }
//        double res = Math.floor(16383 * y);
//        return (int) clamp(res, 0, 16383);
//    }
//    private int mapHold(double source) {
//        source = clamp(source, 0, 250);
//        double res = midiMap.getRangeScaleValue(source / 2.5);
//        return (int) clamp(res, 0, 16383);
//    }
//    private int mapRatio(double source) {
//        System.out.println(source);
//        if (source <= 0) { return 0; }
//        if (source <= 1) { return 1856; }
//        if (source <= 2) { return 3264; }
//        if (source <= 3) { return 4288; }
//        if (source <= 4) { return 5248; }
//        if (source <= 5) { return 5888; }
//        if (source <= 6) { return 6560; }
//        if (source <= 7) { return 7648; }
//        if (source <= 8) { return 8784; }
//        if (source <= 9) { return 9824; }
//        if (source <= 10) { return 11144; }
//        if (source <= 11) { return 13104; }
//        if (source <= 12) { return 15217; }
//        return 16383;
//    }
}
