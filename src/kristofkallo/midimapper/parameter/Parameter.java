package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;

public abstract class Parameter {
    protected String name;
    protected Address address;
    protected int bytes;
    protected boolean signed;
    protected double min;
    protected double max;
    protected double dMin;
    protected double dMax;

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

    public double getDMin() {
        return dMin;
    }

    public double getDMax() {
        return dMax;
    }

    public abstract int mapConsoleToDAW(int source);

    public abstract int mapDAWToConsole(int source);

    protected static double clamp(double number, double min, double max) {
        // handle inverted intervals
        if (max < min) {
            double tmp = min;
            min = max;
            max = tmp;
        }
        if (number < min) {
            number = min;
        }
        if (number > max) {
            number = max;
        }
        return number;
    }

    protected double clampSource(double source) {
        double sourceClamped = clamp(source, min, max);
        sourceClamped = clamp(sourceClamped, dMin, dMax);
        return sourceClamped;
    }
}
