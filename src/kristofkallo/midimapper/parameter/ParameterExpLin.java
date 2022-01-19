package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterExpLin extends Parameter {
    private final double threshold;
    private final double base;
    private final double coefficient;
    public ParameterExpLin(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax, double threshold, double base, double coefficient) {
        this.name = name;
        this.address = address;
        this.bytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
        this.threshold = threshold;
        this.base = base;
        this.coefficient = coefficient;
    }
    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = clampSource(source);
        double res;
        if (sourceClamped < threshold) {
            res = Math.floor(16383 * coefficient * Math.pow(base, sourceClamped));
        } else {
            res = Math.floor(16383 * (sourceClamped - dMin) / (dMax - dMin));
        }
        return (int) clamp(res, 0, 16383);
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
