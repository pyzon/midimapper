package kristofkallo.midimapper;

public class Parameter {
    private final String name;
    private final Address address;
    private final double min;
    private final double max;
    private final int bytes;
    private final boolean signed;
    private final Scale scale;
    private final double dMin;
    private final double dMax;

    public Parameter(String name, Address address, double min, double max, int bytes, boolean signed, Scale scale, double dMin, double dMax) {
        this.name = name;
        this.address = address;
        this.min = min;
        this.max = max;
        this.bytes = bytes;
        this.signed = signed;
        this.scale = scale;
        this.dMin = dMin;
        this.dMax = dMax;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getBytes() {
        return bytes;
    }

    public boolean isSigned() {
        return signed;
    }

    public Scale getScale() {
        return scale;
    }

    public double getDMin() {
        return dMin;
    }

    public double getDMax() {
        return dMax;
    }
}
