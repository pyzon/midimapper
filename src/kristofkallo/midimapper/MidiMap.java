package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MidiMap {
    private final ArrayList<Channel> channels;

    MidiMap(String pathname) throws ParserConfigurationException, IOException, SAXException {
        channels = new ArrayList<>();

        File mapFile = new File(pathname);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(mapFile);
        document.getDocumentElement().normalize();

        // Get the scale info before the parameters, because the parameter constructors need them
        Map<String, ScalePoints> scalePointsMap = new HashMap<>(2);

        NodeList scaleNodes = document.getElementsByTagName("scale");
        for (int i = 0; i < scaleNodes.getLength(); i++) {
            Node scaleNode = scaleNodes.item(i);
            String scaleId = scaleNode.getAttributes().getNamedItem("id").getNodeValue();
            NodeList scalePointNodes = scaleNode.getChildNodes();
            scalePointsMap.put(scaleId, this.loadScale(scalePointNodes));
        }

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
                Parameter parameter = MidiMap.readParameter(paramNode, scalePointsMap);
                channel.putParameter(parameter);

                paramNode = paramNode.getNextSibling();
            }

            channels.add(channel);
        }

    }

    private static Parameter readParameter(Node paramNode, Map<String, ScalePoints> scalePointsMap) throws SAXException {
        String paramName = paramNode.getAttributes().getNamedItem("name").getNodeValue();

        Node addressNode = paramNode.getFirstChild();
        while (!addressNode.getNodeName().equals("address")) {
            addressNode = addressNode.getNextSibling();
        }
        NamedNodeMap addressAttributes = addressNode.getAttributes();
        byte sysex0 = Byte.parseByte(addressAttributes.getNamedItem("sysex0").getNodeValue(), 16);
        byte sysex1 = Byte.parseByte(addressAttributes.getNamedItem("sysex1").getNodeValue(), 16);
        byte nrpn = Byte.parseByte(addressAttributes.getNamedItem("nrpn").getNodeValue(), 16);
        Address paramAddress = new Address(sysex0, sysex1, nrpn);

        Node dataNode = addressNode.getNextSibling();
        while (!dataNode.getNodeName().equals("data")) {
            dataNode = dataNode.getNextSibling();
        }
        NamedNodeMap dataAttributes = dataNode.getAttributes();
        int bytes = Integer.parseInt(dataAttributes.getNamedItem("bytes").getNodeValue());
        String scaleStr = dataAttributes.getNamedItem("scale").getNodeValue();
        Scale scale = Scale.valueOf(scaleStr);

        Parameter parameter;

        Node signedAttr = dataAttributes.getNamedItem("signed");
//        boolean signed = (scale == Scale.FADER || scale == Scale.RANGE);
        boolean signed = signedAttr != null && Boolean.parseBoolean(signedAttr.getNodeValue());
        Node minAttr = dataAttributes.getNamedItem("min");
        double min = minAttr == null ? 0 : Double.parseDouble(minAttr.getNodeValue());
        Node maxAttr = dataAttributes.getNamedItem("max");
        double max = maxAttr == null ? 0 : Double.parseDouble(maxAttr.getNodeValue());
        Node dMinAttr = dataAttributes.getNamedItem("dmin");
        double dMin = dMinAttr == null ? 0 : Double.parseDouble(dMinAttr.getNodeValue());
        Node dMaxAttr = dataAttributes.getNamedItem("dmax");
        double dMax = dMaxAttr == null ? 0 : Double.parseDouble(dMaxAttr.getNodeValue());

        String scaleId = MidiMap.getScaleId(paramName);

        switch (scale) {
            case LIN:
                parameter = new ParameterLinear(paramName, paramAddress, bytes, signed, min, max, dMin, dMax);
                break;
            case SW:
                parameter = new ParameterSwitch(paramName, paramAddress);
                break;
            case LOG:
                parameter = new ParameterLog(paramName, paramAddress, bytes, signed, min, max, dMin, dMax);
                break;
            case SPLINE:
                parameter = new ParameterSpline(paramName, paramAddress, bytes, signed, min, max, dMin, dMax, scalePointsMap.get(scaleId));
                break;
            case POW:
                Node expAttr = dataAttributes.getNamedItem("exp");
                double exp = expAttr == null ? 0 : Double.parseDouble(expAttr.getNodeValue());
                parameter = new ParameterPower(paramName, paramAddress, bytes, signed, min, max, dMin, dMax, exp);
                break;
            case STAIRS:
                parameter = new ParameterStairs(paramName, paramAddress, scalePointsMap.get(scaleId));
                break;
            case POLY:
                parameter = new ParameterPolygonal(paramName, paramAddress, bytes, signed, min, max, dMin, dMax, scalePointsMap.get(scaleId));
                break;
            default:
                throw new SAXException("Unimplemented scale type.");
        }
        return parameter;
    }

    public Channel getChannelByAddress(byte address0, byte address1) {
        return channels.stream()
                .filter(ch -> ch.getAddress().getSysex0() == address0 && ch.getAddress().getSysex1() == address1)
                .findAny()
                .orElse(null);
    }
    private ScalePoints loadScale(NodeList scalePointNodes) {
        ArrayList<Node> pointNodes = new ArrayList<>();
        for (int i = 0; i < scalePointNodes.getLength(); i++) {
            Node pointNode = scalePointNodes.item(i);
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
        return new ScalePoints(x, y);
    }

    private static String getScaleId(String name) {
        return "scale-" + name.replace(' ', '-');
    }

//    public double getRangeScaleValue(double x) {
//        return rangeScale.value(x);
//    }
}
