package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import kristofkallo.midimapper.ScalePoints;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ParameterSpline extends Parameter {
    private final PolynomialSplineFunction splineFunction;

    public ParameterSpline(String name, Address address, int bytes, boolean signed, double min, double max, double dMin, double dMax, ScalePoints scalePoints) {
        this.name = name;
        this.address = address;
        this.bytes = bytes;
        this.signed = signed;
        this.min = min;
        this.max = max;
        this.dMin = dMin;
        this.dMax = dMax;
        SplineInterpolator interpolator = new SplineInterpolator();
        this.splineFunction = scalePoints == null ? null : interpolator.interpolate(scalePoints.x, scalePoints.y);
    }

    private double getSplineFunctionValue(double x) {
        return splineFunction.value(x);
    }

    @Override
    public int mapConsoleToDAW(int source) {
        double sourceClamped = clampSource(source);
        double res = getSplineFunctionValue(sourceClamped);
        return (int) clamp(res, 0, 16383);
    }

    @Override
    public int mapDAWToConsole(int source) {
        throw new NotImplementedException();
    }
}
