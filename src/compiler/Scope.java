package compiler;

import java.util.HashMap;
import java.util.Map;

public class Scope {
    String name;
    int scopeNumber;
    Scope parent;
    Attributes attributes;
    private HashMap<String, Attributes> symbolTable = new HashMap<>();

    public Scope(String name,int scopeNumber){
        this.name = name;
        this.scopeNumber = scopeNumber;
    }

    public void setParent(Scope parent) {
        this.parent = parent;
    }
    public Scope getParent(){
        return parent;
    }

    public void insert(String keyName, Attributes attributes) {
        symbolTable.put(keyName, attributes);
    }

    public Attributes lookup(String keyName) {
        return symbolTable.get(keyName);
    }

    public String printItems() {
        String itemsStr = "";
        for (Map.Entry<String, Attributes> entry : symbolTable.entrySet()) {
            itemsStr += "Key = " + entry.getKey() + " | Value = " + entry.getValue()
                    + "\n";
        }
        return itemsStr;
    }

    public String toString() {
        return "------------- " + name + " : " + scopeNumber + " -------------\n" +
                printItems() +
                "-----------------------------------------\n";
    }
}
