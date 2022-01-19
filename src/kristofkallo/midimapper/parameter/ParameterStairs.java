package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import kristofkallo.midimapper.ScalePoints;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterStairs extends Parameter {
    private final ScalePoints scalePoints;
    public ParameterStairs(String name, Address address, ScalePoints scalePoints) {
        this.name = name;
        this.address = address;
        this.bytes = 1;
        this.signed = false;
        this.min = 0;
        this.max = 1;
        this.dMin = 0;
        this.dMax = 1;
        this.scalePoints = scalePoints;
    }

    @Override
    public int mapConsoleToDAW(int source) {
        for (int i = 0; i < scalePoints.x.length; i++) {
            if (source <= scalePoints.x[i]) {
                return (int) clamp(scalePoints.y[i], 0, 16383);
            }
        }
        return 16383;
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
