package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterLog extends Parameter {
    public ParameterLog(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax) {
        this.name = name;
        this.address = address;
        this.bytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
    }
    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = this.clampSource(source);
        return (int) Math.floor((double) 16383 * Math.log(sourceClamped / this.dMin) / Math.log(this.dMax / this.dMin));
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
