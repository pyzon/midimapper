package kristofkallo.midimapper;

public class LineStripFunction {
    private final double[] x;
    private final double[] y;

    public LineStripFunction(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("the number of x and y coordinates should equal");
        }
        this.x = x;
        this.y = y;
    }

    public double value(double v) {
        int index = this.findIntervalIndex(v);
        if (index == x.length - 1) {
            return y[x.length - 1];
        }
        double y1 = y[index];
        double y2 = y[index + 1];
        double x1 = x[index];
        double x2 = x[index + 1];
        double m = (y2 - y1) / (x2 - x1);
        return m * v - m * x1 + y1;
    }

    private int findIntervalIndex(double v) {
        if (v < x[0] || x[x.length - 1] < v) {
            throw new IllegalArgumentException(String.format("argument [%s] out of range", v));
        }
        for (int i = 1; i < x.length; i++) {
            if (v < x[i]) {
                return i - 1;
            }
        }
        return x.length - 1;
    }
}
