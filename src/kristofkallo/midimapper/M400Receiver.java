package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.Arrays;

import static kristofkallo.midimapper.MidiDataTransform.fromByteArraySigned;
import static kristofkallo.midimapper.MidiDataTransform.toByteArray;


public class M400Receiver implements Receiver {
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
            if (M400ByteCode.SYS_EX_STATUS_BYTE.is(msg[0]) &&
                    M400ByteCode.MANUFACTURER_ID.is(msg[1]) &&
                    M400ByteCode.DEVICE_ID.is(msg[2]) &&
                    M400ByteCode.MODEL_ID_0.is(msg[3]) &&
                    M400ByteCode.MODEL_ID_1.is(msg[4]) &&
                    M400ByteCode.MODEL_ID_2.is(msg[5]) &&
                    M400ByteCode.DATA_SET_COMMAND_ID.is(msg[6])) {
                Channel channel = midiMap.getChannelByAddress(msg[7], msg[8]);
                if (channel == null) {
                    // Channel unmapped, i.e. unused
                    return;
                }

                Parameter param = channel.getParameterByAddress(msg[9], msg[10]);
                if (param == null) {
                    // Parameter unmapped, i.e. unused
                    return;
                }

                byte[] srcData = Arrays.copyOfRange(msg, 11, 11 + param.getBytes());

                byte[] dstData = toByteArray(
                        param.mapConsoleToDAW(
                                fromByteArraySigned(srcData)
                        ), 2
                );

                ShortMessage outMsg = new ShortMessage();
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 99, channel.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 98, param.getAddress().getNrpn());
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 6, dstData[0]);
                receiver.send(outMsg, timeStamp);
                outMsg.setMessage(ShortMessage.CONTROL_CHANGE, 0, 38, dstData[1]);
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
