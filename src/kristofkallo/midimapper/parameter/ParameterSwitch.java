package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterSwitch extends Parameter {
    public ParameterSwitch(String name, Address address) {
        this.name = name;
        this.address = address;
        this.bytes = 1;
        this.signed = false;
        this.min = 0;
        this.max = 1;
        this.dMin = 0;
        this.dMax = 1;
    }

    @Override
    public int mapConsoleToDAW(int source) {
        return source == 0 ? 0 : 16383;
    }

    @Override
    public int mapDAWToConsole(int source) {
        return source == 0 ? 0 : 1;
    }
}
