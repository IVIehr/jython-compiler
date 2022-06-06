package compiler;

public class Attributes {
    String attributeType, name, classType;
    boolean isDefined;
    String parameter, returnType;
    String classTypeParent;
    boolean variable = false, method = false, classs = false, constructor = false;

    public Attributes(String attributeType, String name, String classType, Boolean isDefined) {
        this.attributeType = attributeType;
        this.name = name;
        this.classType = classType;
        this.isDefined = isDefined;
        variable = true;
    }

    public Attributes(String attributeType, String name, String parameter, String returnType) {
        this.attributeType = attributeType;
        this.parameter = parameter;
        this.returnType = returnType;
        method = true;
    }

    public Attributes(String attributeType, String name, String classTypeParent) {
        this.attributeType = attributeType;
        this.name = name;
        this.classTypeParent = classTypeParent;
        classs = true;
    }

    public Attributes(String name, String parameter) {
        this.name = name;
        this.parameter = parameter;
        constructor = true;
    }

    public String toString() {
        if (variable)
            return attributeType + " (name: " + name + ")" + "(ClassType: " + classType + ", isDefined: " + isDefined + ")";
        else if (method)
            return attributeType + " (name: " + name + ")" + parameter + "(return type: " + returnType + ")";
        else if (constructor) return "Constructor (name: " + name + ")" + parameter;
        else return attributeType + " (name: " + name + ")" + classTypeParent;
    }
}
