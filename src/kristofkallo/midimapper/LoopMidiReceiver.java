package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

import javax.sound.midi.*;

import static kristofkallo.midimapper.MidiDataTransform.fromByteArrayUnsigned;
import static kristofkallo.midimapper.MidiDataTransform.toByteArray;

public class LoopMidiReceiver implements Receiver {
    private final Receiver receiver;
    private final MidiMap midiMap;

    private byte channelNrpn;
    private byte paramNrpn;
    private byte valueMSB;

    /**
     * Keeps track of the nrpn message parts to ensure the correct order.
     */
    private int nrpnStage = 0;

    public LoopMidiReceiver(Receiver receiver, MidiMap midiMap) {
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
        try {
            if (msg[0] == (byte) ShortMessage.CONTROL_CHANGE) {
                switch (msg[1]) {
                    case (byte) 99:
                        nrpnStage = 99;
                        channelNrpn = msg[2];
                        break;
                    case (byte) 98:
                        if (nrpnStage != 99) {
                            nrpnStage = 0;
                            break;
                        }
                        nrpnStage = 98;
                        paramNrpn = msg[2];
                        break;
                    case (byte) 6:
                        if (nrpnStage != 98) {
                            nrpnStage = 0;
                            break;
                        }
                        nrpnStage = 6;
                        valueMSB = msg[2];
                        break;
                    case (byte) 38:
                        if (nrpnStage != 6) {
                            nrpnStage = 0;
                            break;
                        }
                        System.out.println("38 arrived on schedule, sending sysex");
                        nrpnStage = 38;
                        byte valueLSB = msg[2];

                        Channel channel = midiMap.getChannelByNrpn(channelNrpn);
                        if (channel == null) {
                            System.err.println("channel unmapped in message from DAW, " +
                                    "this shouldn't happen unless the DAW setting " +
                                    "conflicts the actual mapping");
                            return;
                        }

                        Parameter param = channel.getParameterByNrpn(paramNrpn);
                        if (param == null) {
                            System.err.println("parameter unmapped in message from DAW, " +
                                    "this shouldn't happen unless the DAW setting " +
                                    "conflicts the actual mapping");
                            return;
                        }

                        byte[] srcData = new byte[]{valueMSB, valueLSB};

                        int dstDataLen = param.getBytes();
                        byte[] dstData = toByteArray(
                                param.mapDAWToConsole(
                                        fromByteArrayUnsigned(srcData)
                                ), dstDataLen
                        );

                        // build the sysex message
                        int outMsgLen = 13 + dstDataLen;
                        byte[] outMsgData = new byte[outMsgLen];
                        outMsgData[0] = M400ByteCode.SYS_EX_STATUS_BYTE.getCode();
                        outMsgData[1] = M400ByteCode.MANUFACTURER_ID.getCode();
                        outMsgData[2] = M400ByteCode.DEVICE_ID.getCode();
                        outMsgData[3] = M400ByteCode.MODEL_ID_0.getCode();
                        outMsgData[4] = M400ByteCode.MODEL_ID_1.getCode();
                        outMsgData[5] = M400ByteCode.MODEL_ID_2.getCode();
                        outMsgData[6] = M400ByteCode.DATA_SET_COMMAND_ID.getCode();
                        outMsgData[7] = channel.getAddress().getSysex0();
                        outMsgData[8] = channel.getAddress().getSysex1();
                        outMsgData[9] = param.getAddress().getSysex0();
                        outMsgData[10] = param.getAddress().getSysex1();
                        System.arraycopy(dstData, 0, outMsgData, 11, dstDataLen);
                        int checksum = 0;
                        for (int i = 7; i < 11 + dstDataLen; i++) {
                            checksum += outMsgData[i];
                        }
                        outMsgData[11 + dstDataLen] = (byte) ((128 - checksum) & 127);
                        outMsgData[12 + dstDataLen] = M400ByteCode.EOX.getCode();

//                        for (byte data : outMsgData) {
//                            System.out.println(data);
//                        }

                        SysexMessage outMsg = new SysexMessage(outMsgData, outMsgLen);
                        receiver.send(outMsg, timeStamp);

                        break;
                    default:
                        nrpnStage = 0;
                }
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
