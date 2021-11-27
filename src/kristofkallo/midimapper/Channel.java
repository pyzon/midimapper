package kristofkallo.midimapper;

import kristofkallo.midimapper.parameter.Parameter;

import java.util.ArrayList;

public class Channel {
    private final String id; // TODO: id and name probably not needed
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

    public Parameter getParameterByAddress(byte address0, byte address1) {
        return parameters.stream()
                .filter(p -> p.getAddress().getSysex0() == address0 && p.getAddress().getSysex1() == address1)
                .findAny()
                .orElse(null);
    }
    public void putParameter(Parameter parameter) {
        parameters.add(parameter);
    }
}
