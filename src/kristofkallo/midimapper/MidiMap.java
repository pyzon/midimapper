package kristofkallo.midimapper;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MidiMap {
    private ArrayList<Channel> channels;

    MidiMap(String pathname) throws ParserConfigurationException, IOException, SAXException {
        File mapFile = new File(pathname);
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(mapFile);
        document.getDocumentElement().normalize();
        document.getDocumentElement().getElementsByTagName("channel"); //...
    }
}
