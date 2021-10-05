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
                byte[] data = Arrays.copyOfRange(msg, 11, 11 + param.getBytes());
                int value = 0;
                switch (param.getScale()) {
                    case LIN:
                        value = mapLin(squash(data), param.getMin(), param.getMax(), param.getDMin(), param.getDMax());
                        break;
                    case LOG:
                        value = mapLog(squash(data), param.getMin(), param.getMax(), param.getDMin(), param.getDMax());
                        break;
                    case RATIO:
                        return;
                }

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
    private double clamp(double number, double min, double max) {
        if (number < min) {
            number = min;
        }
        if (number > max) {
            number = max;
        }
        return  number;
    }
    private int mapLog(double source, double sourceMin, double sourceMax, double destMin, double destMax) {
        source = clamp(source, sourceMin, sourceMax);
        source = clamp(source, destMin, destMax);
        return (int) Math.round((double) 16384 * Math.log(source/destMin) / Math.log(destMax/destMin));
    }
    private int mapLin(double source, double sourceMin, double sourceMax, double destMin, double destMax) {
        source = clamp(source, sourceMin, sourceMax);
        source = clamp(source, destMin, destMax);
        return (int) Math.round(16384 * (source - destMin) / (destMax - destMin));
    }
}
