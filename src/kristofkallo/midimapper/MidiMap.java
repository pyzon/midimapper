package kristofkallo.midimapper;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;

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
                int min = Integer.parseInt(dataAttributes.getNamedItem("min").getNodeValue());
                int max = Integer.parseInt(dataAttributes.getNamedItem("max").getNodeValue());
                int bytes = Integer.parseInt(dataAttributes.getNamedItem("bytes").getNodeValue());
                Scale scale = Scale.valueOf(dataAttributes.getNamedItem("scale").getNodeValue());
                int dMin = Integer.parseInt(dataAttributes.getNamedItem("dmin").getNodeValue());
                int dMax = Integer.parseInt(dataAttributes.getNamedItem("dmax").getNodeValue());

                Parameter parameter = new Parameter(paramName, paramAddress, min, max, bytes, scale, dMin, dMax);
                channel.putParameter(parameter);

                paramNode = paramNode.getNextSibling();
            }

            channels.add(channel);
        }
    }
    public Channel getChannelByAddress(byte address0, byte address1) {
        return channels.stream()
                .filter(ch -> ch.getAddress().getSysex0() == address0 && ch.getAddress().getSysex1() == address1)
                .findAny()
                .orElse(null);
    }
}
