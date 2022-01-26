package kristofkallo.midimapper.parameter;

import kristofkallo.midimapper.Address;
import kristofkallo.midimapper.Scale;
import kristofkallo.midimapper.ScalePoints;
import org.xml.sax.SAXException;

public class ParameterFactory {
    public Parameter createParameter(
            Scale scale,
            String name,
            Address address,
            int bytes,
            boolean signed,
            double min,
            double max,
            double dMin,
            double dMax,
            ScalePoints scalePoints,
            double exponent,
            double threshold,
            double base,
            double coefficient) throws SAXException {
        Parameter parameter;
        switch (scale) {
            case LIN:
                parameter = new ParameterLinear(name, address, bytes, signed, min, max, dMin, dMax);
                break;
            case SW:
                parameter = new ParameterSwitch(name, address);
                break;
            case LOG:
                parameter = new ParameterLog(name, address, bytes, signed, min, max, dMin, dMax);
                break;
            case SPLINE:
                parameter = new ParameterSpline(name, address, bytes, signed, min, max, dMin, dMax, scalePoints);
                break;
            case POW:
                parameter = new ParameterPower(name, address, bytes, signed, min, max, dMin, dMax, exponent);
                break;
            case POWLIN:
                parameter = new ParameterPowLin(name, address, bytes, signed, min, max, dMin, dMax, exponent, threshold, coefficient);
                break;
            case STAIRS:
                parameter = new ParameterStairs(name, address, scalePoints);
                break;
            case POLY:
                parameter = new ParameterPolygonal(name, address, bytes, signed, min, max, dMin, dMax, scalePoints);
                break;
            case EXPLIN:
                parameter = new ParameterExpLin(name, address, bytes, signed, min, max, dMin, dMax, threshold, base, coefficient);
                break;
            default:
                throw new SAXException("Unimplemented scale type.");
        }
        return parameter;
    }
}
