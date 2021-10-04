package kristofkallo.midimapper;

import java.util.ArrayList;
import java.util.HashMap;

public class Channel {
    private final String id;
    private final String name;
    private final Address address;
    private final ArrayList<Parameter> parameters;

    public Channel(String id, String name, Address address) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.parameters = new ArrayList<>();
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public Address getAddress() {
        return address;
    }

    public Parameter getParameter(int index) {
        return parameters.get(index);
    }
    public void putParameter(Parameter parameter) {
        parameters.add(parameter);
    }
}
