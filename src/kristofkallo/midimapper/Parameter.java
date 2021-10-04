package kristofkallo.midimapper;

public class Parameter {
    private final String name;
    private final Address address;
    private final int min;
    private final int max;
    private final int bytes;
    private final Scale scale;
    private final int dMin;
    private final int dMax;

    public Parameter(String name, Address address, int min, int max, int bytes, Scale scale, int dMin, int dMax) {
        this.name = name;
        this.address = address;
        this.min = min;
        this.max = max;
        this.bytes = bytes;
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

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getBytes() {
        return bytes;
    }

    public Scale getScale() {
        return scale;
    }

    public int getdMin() {
        return dMin;
    }

    public int getdMax() {
        return dMax;
    }
}
