package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Arrays;

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
            throw new NullPointerException("receiver is null, this should not happen");
        }
        if (midiMap == null) {
            throw new NullPointerException("midiMap is null, this should not happen");
        }

        byte[] msg = message.getMessage();
        // SysEx status byte f0 which is -16
        try {
            if (msg[0] == SYS_EX_STATUS_BYTE &&
                    msg[1] == MANUFACTURER_ID &&
                    msg[2] == DEVICE_ID &&
                    Arrays.equals(Arrays.copyOfRange(msg, 3, 6), MODEL_ID) &&
                    msg[6] == DATA_SET_COMMAND_ID) {
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
                int value = param.mapConsoleToDAW(
                        param.isSigned() ?
                                MidiData.fromByteArraySigned(data).getData() :
                                MidiData.fromByteArrayUnsigned(data).getData()
                );

                byte[] valueParts = new MidiData(value).toByteArray(2);

                ShortMessage outMsg = new ShortMessage();
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 99, channel.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 98, param.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 6, valueParts[0]);
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 38, valueParts[1]);
                receiver.send(outMsg, timeStamp);

            }
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("MIDI message too short");
        }
    }

    @Override
    public void close() {

    }
}
