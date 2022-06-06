package compiler;

public class Parameter {
    String type, name;

    public Parameter(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String toString() {
        return type + " " + name;
    }
}
