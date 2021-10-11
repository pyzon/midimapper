package kristofkallo.midimapper;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MidiMap {
    private final ArrayList<Channel> channels;
    private PolynomialSplineFunction faderScale;

    MidiMap(String pathname) throws ParserConfigurationException, IOException, SAXException {
        channels = new ArrayList<>();

        File mapFile = new File(pathname);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(mapFile);
        document.getDocumentElement().normalize();

        // Channels
        NodeList channelNodes = document.getElementsByTagName("channel");
        for(int i = 0; i < channelNodes.getLength(); i++) {
            Node channelNode = channelNodes.item(i);
            NamedNodeMap channelAttributes = channelNode.getAttributes();
            String id = channelAttributes.getNamedItem("id").getNodeValue();
            String name = channelAttributes.getNamedItem("name").getNodeValue();

            Node addressNode = channelNode.getFirstChild();
            while (!addressNode.getNodeName().equals("address")) {
                addressNode = addressNode.getNextSibling();
            }
            NamedNodeMap addressAttributes = addressNode.getAttributes();
            byte sysex0 = Byte.parseByte(addressAttributes.getNamedItem("sysex0").getNodeValue(), 16);
            byte sysex1 = Byte.parseByte(addressAttributes.getNamedItem("sysex1").getNodeValue(), 16);
            byte nrpn = Byte.parseByte(addressAttributes.getNamedItem("nrpn").getNodeValue(), 16);
            Address channelAddress = new Address(sysex0, sysex1, nrpn);

            Channel channel = new Channel(id, name, channelAddress);

            // Parameters
            Node paramNode = addressNode.getNextSibling();
            while (paramNode != null) {
                if (!paramNode.getNodeName().equals("parameter")) {
                    paramNode = paramNode.getNextSibling();
                    continue;
                }
                String paramName = paramNode.getAttributes().getNamedItem("name").getNodeValue();

                addressNode = paramNode.getFirstChild();
                while (!addressNode.getNodeName().equals("address")) {
                    addressNode = addressNode.getNextSibling();
                }
                addressAttributes = addressNode.getAttributes();
                sysex0 = Byte.parseByte(addressAttributes.getNamedItem("sysex0").getNodeValue(), 16);
                sysex1 = Byte.parseByte(addressAttributes.getNamedItem("sysex1").getNodeValue(), 16);
                nrpn = Byte.parseByte(addressAttributes.getNamedItem("nrpn").getNodeValue(), 16);
                Address paramAddress = new Address(sysex0, sysex1, nrpn);

                Node dataNode = addressNode.getNextSibling();
                while (!dataNode.getNodeName().equals("data")) {
                    dataNode = dataNode.getNextSibling();
                }
                NamedNodeMap dataAttributes = dataNode.getAttributes();
                int bytes = Integer.parseInt(dataAttributes.getNamedItem("bytes").getNodeValue());
                boolean signed = Boolean.parseBoolean(dataAttributes.getNamedItem("signed").getNodeValue());
                Scale scale = Scale.valueOf(dataAttributes.getNamedItem("scale").getNodeValue());
                Node minAttr = dataAttributes.getNamedItem("min");
                double min = minAttr == null ? 0 : Double.parseDouble(minAttr.getNodeValue());
                Node maxAttr = dataAttributes.getNamedItem("max");
                double max = maxAttr == null ? 0 : Double.parseDouble(maxAttr.getNodeValue());
                Node dMinAttr = dataAttributes.getNamedItem("dmin");
                double dMin = dMinAttr == null ? 0 : Double.parseDouble(dMinAttr.getNodeValue());
                Node dMaxAttr = dataAttributes.getNamedItem("dmax");
                double dMax = dMaxAttr == null ? 0 : Double.parseDouble(dMaxAttr.getNodeValue());

                Parameter parameter = new Parameter(paramName, paramAddress, min, max, bytes, signed, scale, dMin, dMax);
                channel.putParameter(parameter);

                paramNode = paramNode.getNextSibling();
            }

            channels.add(channel);
        }

        NodeList faderScalePointNodes = document.getElementsByTagName("scale").item(0).getChildNodes();
        ArrayList<Node> pointNodes = new ArrayList<>();
        for (int i = 0; i < faderScalePointNodes.getLength(); i++) {
            Node pointNode = faderScalePointNodes.item(i);
            if (!pointNode.getNodeName().equals("point")) {
                continue;
            }
            pointNodes.add(pointNode);
        }
        int n = pointNodes.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            NamedNodeMap pointAttributes = pointNodes.get(i).getAttributes();
            x[i] = Double.parseDouble(pointAttributes.getNamedItem("x").getNodeValue());
            y[i] = Double.parseDouble(pointAttributes.getNamedItem("y").getNodeValue());
        }
        SplineInterpolator interpolator = new SplineInterpolator();
        faderScale = interpolator.interpolate(x, y);
    }
    public Channel getChannelByAddress(byte address0, byte address1) {
        return channels.stream()
                .filter(ch -> ch.getAddress().getSysex0() == address0 && ch.getAddress().getSysex1() == address1)
                .findAny()
                .orElse(null);
    }
    public double getFaderScaleValue(double x) {
        return faderScale.value(x);
    }
}
