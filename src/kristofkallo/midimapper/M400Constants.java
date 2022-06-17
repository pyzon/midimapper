package kristofkallo.midimapper;

public enum M400Constants {
    SYS_EX_STATUS_BYTE ((byte) 0xf0);

    private final byte code;
    M400Constants(byte code) {
        this.code = code;
    }
}
