package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import kristofkallo.midimapper.PolygonalFunction;
import kristofkallo.midimapper.ScalePoints;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterPolygonal extends Parameter {
    private final PolygonalFunction polygonalFunction;

    public ParameterPolygonal(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax, ScalePoints scalePoints) {
        this.name = name;
        this.address = address;
        this.bytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
        polygonalFunction = new PolygonalFunction(scalePoints.x, scalePoints.y);
    }

    private double getPolygonalFunctionValue(double x) {
        return polygonalFunction.value(x);
    }

    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = clampSource(source);
        double res = getPolygonalFunctionValue(sourceClamped);
        return (int) clamp(res, 0, 16383);
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }

}
