package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterLinear extends Parameter {
    public ParameterLinear(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax) {
        this.name = name;
        this.address = address;
        this.lengthInBytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
    }

    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = clampSource(source);
        return (int) Math.floor(16383 * (sourceClamped - dMin) / (dMax - dMin));
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
