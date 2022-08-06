package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterPowLin extends Parameter {
    private final double exponent;
    private final double threshold;
    private final double coefficient;
    public ParameterPowLin(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax, double exponent, double threshold, double coefficient) {
        this.name = name;
        this.address = address;
        this.lengthInBytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
        this.exponent = exponent;
        this.threshold = threshold;
        this.coefficient = coefficient;
    }
    @Override
    public int mapConsoleToDAW(int source) {

//        double y;
//        if (source < 2500) {
//            double x = source / 2500;
//            y = 0.9 * Math.pow(x, 0.3333333333333);
//        } else {
//            y = 0.9 + (source - 2500) / 2500 * 0.1;
//        }
//        double res = Math.floor(16383 * y);
//        return (int) clamp(res, 0, 16383);

        double sourceClamped = clampSource(source);
        double res;
        if (sourceClamped < threshold) {
            double x = (sourceClamped - dMin) / (threshold - dMin);
            res = Math.floor(16383 * coefficient * Math.pow(x, exponent));
        } else {
            double x = coefficient + (1 - coefficient) * (sourceClamped - threshold) / (dMax - threshold);
            res = Math.floor(16383 * x);
        }
        return (int) clamp(res, 0, 16383);
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
