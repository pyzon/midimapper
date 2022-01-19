package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterPower extends Parameter {
    private double exponent;

    public ParameterPower(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax, double exponent) {
        this.name = name;
        this.address = address;
        this.bytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
        this.exponent = exponent;
    }

    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = clampSource(source);
        double x = (source - dMin) / (dMax - dMin);
        double y = Math.pow(x, exponent);
        double res = Math.floor(16383 * y);
        return (int) clamp(res, 0, 16383);
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
