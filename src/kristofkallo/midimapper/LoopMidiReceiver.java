package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

import javax.sound.midi.*;

import static kristofkallo.midimapper.MidiDataTransform.fromByteArrayUnsigned;
import static kristofkallo.midimapper.MidiDataTransform.toByteArray;

public class LoopMidiReceiver implements Receiver {
    public static final int NRPN_STAGE_BEGIN = 0;
    public static final int NRPN_STAGE_CHANNEL = 99;
    public static final int NRPN_STAGE_PARAM = 98;
    public static final int NRPN_STAGE_VALUE_MSB = 6;
    public static final int NRPN_STAGE_VALUE_LSB = 38;

    private final Receiver receiver;
    private final MidiMap midiMap;

    private byte channelNrpn;
    private byte paramNrpn;
    private byte valueMSB;
    private byte valueLSB;

    private Channel channel;
    private Parameter param;

    /**
     * Keeps track of the nrpn message parts to ensure the correct order.
     * For more information on how NRPN works, read this: https://en.wikipedia.org/wiki/NRPN
     */
    private int nrpnStage = 0;

    public LoopMidiReceiver(Receiver receiver, MidiMap midiMap) {
        this.receiver = receiver;
        this.midiMap = midiMap;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        sanityCheck();

        byte[] msg = message.getMessage();
        if (isNotNrpn(msg)) {
            return;
        }
        transformAndForwardNrpnAndHandleErrors(msg, timeStamp);
    }

    private void sanityCheck() {
        if (receiver == null) {
            throw new NullPointerException("receiver is null, this should not happen");
        }
        if (midiMap == null) {
            throw new NullPointerException("midiMap is null, this should not happen");
        }
    }

    private boolean isNotNrpn(byte[] msg) {
        return msg[0] != (byte) ShortMessage.CONTROL_CHANGE;
    }

    private void transformAndForwardNrpnAndHandleErrors(byte[] msg, long timestamp) {
        try {
            transformAndForwardNrpn(msg, timestamp);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("MIDI message too short");
        }
    }

    private void transformAndForwardNrpn(byte[] msg, long timeStamp) throws InvalidMidiDataException, ArrayIndexOutOfBoundsException {
        switch (msg[1]) {
            case (byte) NRPN_STAGE_CHANNEL:
                handleChannelPartOfNrpn(msg[2]);
                break;
            case (byte) NRPN_STAGE_PARAM:
                handleParamPartOfNrpn(msg[2]);
                break;
            case (byte) NRPN_STAGE_VALUE_MSB:
                handleValueMSBPartOfNrpn(msg[2]);
                break;
            case (byte) NRPN_STAGE_VALUE_LSB:
                handleValueLSBPartOfNrpn(msg[2]);

                transformAndForwardMsg(timeStamp);

                break;
            default:
                nrpnStage = NRPN_STAGE_BEGIN;
        }
    }

    private void handleChannelPartOfNrpn(byte _channelNrpn) {
        nrpnStage = NRPN_STAGE_CHANNEL;
        channelNrpn = _channelNrpn;
    }

    private void handleParamPartOfNrpn(byte _paramNrpn) {
        if (nrpnStage != NRPN_STAGE_CHANNEL) {
            nrpnStage = NRPN_STAGE_BEGIN;
            return;
        }
        nrpnStage = NRPN_STAGE_PARAM;
        paramNrpn = _paramNrpn;
    }

    private void handleValueMSBPartOfNrpn(byte _valueMSB) {
        if (nrpnStage != NRPN_STAGE_PARAM) {
            nrpnStage = NRPN_STAGE_BEGIN;
            return;
        }
        nrpnStage = NRPN_STAGE_VALUE_MSB;
        valueMSB = _valueMSB;
    }

    private void handleValueLSBPartOfNrpn(byte _valueLSB) {
        if (nrpnStage != NRPN_STAGE_VALUE_MSB) {
            nrpnStage = NRPN_STAGE_BEGIN;
            return;
        }
        nrpnStage = NRPN_STAGE_VALUE_LSB;
        valueLSB = _valueLSB;
    }

    private void transformAndForwardMsg(long timeStamp) throws InvalidMidiDataException {
        findChannelAndParamFromMidiMap();
        if (channelNotFound() || parameterNotFound()) {
            return;
        }
        byte[] dstData = transformMsg();
        byte[] outMsgData = buildSysexMsg(dstData);
        sendMsg(outMsgData, timeStamp);
    }

    private void findChannelAndParamFromMidiMap() {
        channel = midiMap.getChannelByNrpn(channelNrpn);
        param = channel.getParameterByNrpn(paramNrpn);
    }

    private boolean channelNotFound() {
        if (channel == null) {
            System.err.println("channel unmapped in message from DAW, " +
                    "this shouldn't happen unless the DAW setting " +
                    "conflicts the actual mapping");
            return true;
        }
        return false;
    }

    private boolean parameterNotFound() {
        if (param == null) {
            System.err.println("parameter unmapped in message from DAW, " +
                    "this shouldn't happen unless the DAW setting " +
                    "conflicts the actual mapping");
            return true;
        }
        return false;
    }

    private byte[] transformMsg() {
        byte[] srcData = new byte[]{valueMSB, valueLSB};
        return toByteArray(
                param.mapDAWToConsole(
                        fromByteArrayUnsigned(srcData)
                ), param.getLengthInBytes()
        );
    }

    private byte[] buildSysexMsg(byte[] dstData) {
        int outMsgLen = 13 + param.getLengthInBytes();
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
        System.arraycopy(dstData, 0, outMsgData, 11, param.getLengthInBytes());
        int checksum = 0;
        for (int i = 7; i < 11 + param.getLengthInBytes(); i++) {
            checksum += outMsgData[i];
        }
        outMsgData[11 + param.getLengthInBytes()] = (byte) ((128 - checksum) & 127);
        outMsgData[12 + param.getLengthInBytes()] = M400ByteCode.EOX.getCode();
        return outMsgData;
    }

    private void sendMsg(byte[] outMsgData, long timeStamp) throws InvalidMidiDataException {
        SysexMessage outMsg = new SysexMessage(outMsgData, outMsgData.length);
        receiver.send(outMsg, timeStamp);
    }

    @Override
    public void close() {

    }
}
