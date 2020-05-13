package main;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    private final LinkedHashMap<String, TypeDescriptor> vars;
    private int maxLocalVars;

    public Scope(Scope parentScope) {
        this.vars = new LinkedHashMap<>();
        maxLocalVars = 0;
        if (parentScope != null)
            this.vars.putAll(parentScope.getVars());

    }

    public HashMap<String, TypeDescriptor> getVars() {
        return this.vars;
    }

    public void addVar(String localIdentifier, TypeDescriptor typeDescriptor) {
        if(this.vars.put(localIdentifier, typeDescriptor) == null){
            maxLocalVars++;
        }

    }

    public void setInit(String key, boolean inited){
        TypeDescriptor var = this.vars.get(key);
        if(var != null) {
            var.setInit(inited);
        }
    }

    public void printScopeVars() {
        System.out.println("Size: " + this.vars.size());
        for (Map.Entry<String, TypeDescriptor> cvar : this.vars.entrySet()) {
            System.out.print("[VAR] ");
            System.out.print("Key: " + cvar.getKey());
            System.out.print(" Value: " + cvar.getValue().getTypeIdentifier());
            System.out.println("\n\n");
        }
    }

    public int getMaxLocalVars() {
        return maxLocalVars;
    }
}
