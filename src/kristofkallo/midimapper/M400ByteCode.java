package kristofkallo.midimapper;

import javax.sound.midi.SysexMessage;

public enum M400ByteCode {
    SYS_EX_STATUS_BYTE ((byte) SysexMessage.SYSTEM_EXCLUSIVE),
    MANUFACTURER_ID ((byte) 0x41),
    DEVICE_ID ((byte) 0x00),
    MODEL_ID_0 ((byte) 0x00),
    MODEL_ID_1 ((byte) 0x00),
    MODEL_ID_2 ((byte) 0x24),
    DATA_SET_COMMAND_ID ((byte) 0x12),
    EOX ((byte) SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE);

    private final byte code;
    M400ByteCode(byte code) {
        this.code = code;
    }

    public boolean is(byte other) {
        return other == code;
    }

    public byte getCode() {
        return code;
    }
}
