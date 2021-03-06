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
        channels = readMap(pathname);
    }

    private static ArrayList<Channel> readMap(String pathname) throws ParserConfigurationException, IOException, SAXException {
        ArrayList<Channel> channels = new ArrayList<>();

        File mapFile = new File(pathname);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(mapFile);
        document.getDocumentElement().normalize();

        // Get the scale info before the parameters, because the parameter constructors need them
        Map<String, ScalePoints> scalePointsMap = readScales(document);

        // Channels
        NodeList channelNodes = document.getElementsByTagName("channel");
        for(int i = 0; i < channelNodes.getLength(); i++) {
            Node channelNode = channelNodes.item(i);
            Channel channel = readChannel(channelNode, scalePointsMap);
            channels.add(channel);
        }
        return channels;
    }

    private static Map<String, ScalePoints> readScales(Document document) {
        Map<String, ScalePoints> scalePointsMap = new HashMap<>(2);

        NodeList scaleNodes = document.getElementsByTagName("scale");
        for (int i = 0; i < scaleNodes.getLength(); i++) {
            Node scaleNode = scaleNodes.item(i);
            String scaleId = scaleNode.getAttributes().getNamedItem("id").getNodeValue();
            NodeList scalePointNodes = scaleNode.getChildNodes();
            scalePointsMap.put(scaleId, readScale(scalePointNodes));
        }

        return scalePointsMap;
    }

    private static ScalePoints readScale(NodeList scalePointNodes) {
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

    private static Channel readChannel(Node channelNode, Map<String, ScalePoints> scalePointsMap) throws SAXException {
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
        byte nrpn = Byte.parseByte(addressAttributes.getNamedItem("nrpn").getNodeValue());
        Address channelAddress = new Address(sysex0, sysex1, nrpn);

        Channel channel = new Channel(id, name, channelAddress);

        // Parameters
        Node paramNode = addressNode.getNextSibling();
        while (paramNode != null) {
            if (!paramNode.getNodeName().equals("parameter")) {
                paramNode = paramNode.getNextSibling();
                continue;
            }
            Parameter parameter = readParameter(paramNode, scalePointsMap);
            channel.putParameter(parameter);

            paramNode = paramNode.getNextSibling();
        }

        return channel;
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
        byte nrpn = Byte.parseByte(addressAttributes.getNamedItem("nrpn").getNodeValue());
        Address paramAddress = new Address(sysex0, sysex1, nrpn);

        Node dataNode = addressNode.getNextSibling();
        while (!dataNode.getNodeName().equals("data")) {
            dataNode = dataNode.getNextSibling();
        }
        NamedNodeMap dataAttributes = dataNode.getAttributes();
        int bytes = Integer.parseInt(dataAttributes.getNamedItem("bytes").getNodeValue());
        String scaleStr = dataAttributes.getNamedItem("scale").getNodeValue();
        Scale scale = Scale.valueOf(scaleStr);

        Node signedAttr = dataAttributes.getNamedItem("signed");
        boolean signed = signedAttr != null && Boolean.parseBoolean(signedAttr.getNodeValue());
        Node minAttr = dataAttributes.getNamedItem("min");
        double min = minAttr == null ? 0 : Double.parseDouble(minAttr.getNodeValue());
        Node maxAttr = dataAttributes.getNamedItem("max");
        double max = maxAttr == null ? 0 : Double.parseDouble(maxAttr.getNodeValue());
        Node dMinAttr = dataAttributes.getNamedItem("dmin");
        double dMin = dMinAttr == null ? 0 : Double.parseDouble(dMinAttr.getNodeValue());
        Node dMaxAttr = dataAttributes.getNamedItem("dmax");
        double dMax = dMaxAttr == null ? 0 : Double.parseDouble(dMaxAttr.getNodeValue());
        Node expAttr = dataAttributes.getNamedItem("exp");
        double exp = expAttr == null ? 0 : Double.parseDouble(expAttr.getNodeValue());
        Node threshAttr = dataAttributes.getNamedItem("thresh");
        double thresh = threshAttr == null ? 0 : Double.parseDouble(threshAttr.getNodeValue());
        Node baseAttr = dataAttributes.getNamedItem("base");
        double base = baseAttr == null ? 0 : Double.parseDouble(baseAttr.getNodeValue());
        Node coeffAttr = dataAttributes.getNamedItem("coeff");
        double coeff = coeffAttr == null ? 0 : Double.parseDouble(coeffAttr.getNodeValue());

        String scaleId = MidiMap.getScaleId(paramName);

        ParameterFactory parameterFactory = new ParameterFactory();

        return parameterFactory.createParameter(scale, paramName, paramAddress,
                bytes, signed, min, max, dMin, dMax, scalePointsMap.get(scaleId), exp, thresh, base, coeff);
    }

    public Channel getChannelByAddress(byte address0, byte address1) {
        return channels.stream()
                .filter(ch -> ch.getAddress().getSysex0() == address0 && ch.getAddress().getSysex1() == address1)
                .findAny()
                .orElse(null);
    }
    public Channel getChannelByNrpn(byte nrpn) {
        return channels.stream()
                .filter(ch -> ch.getAddress().getNrpn() == nrpn)
                .findAny()
                .orElse(null);
    }

    private static String getScaleId(String name) {
        name = name.replace(' ', '-');
        // remove digits, so that the different parameters
        // whose names only differ in numbers
        // should have the same scales, e.g. "aux01 send" and "aux02 send"
        name = name.replaceAll("\\d", "");
        return "scale-" + name;
    }
}
