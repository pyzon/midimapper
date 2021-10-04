package kristofkallo.midimapper;

public class Address {
    private final byte sysex0;
    private final byte sysex1;
    private final byte nrpn;
    public Address(byte sysex0, byte sysex1, byte nrpn) {
        this.sysex0 = sysex0;
        this.sysex1 = sysex1;
        this.nrpn = nrpn;
    }

    public byte getSysex0() {
        return sysex0;
    }
    public byte getSysex1() {
        return sysex1;
    }
    public byte getNrpn() {
        return nrpn;
    }
}
