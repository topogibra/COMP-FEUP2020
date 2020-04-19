import java.util.HashMap;
import java.util.Map;

public class Scope {
    private Scope parentScope;
    private HashMap<String, TypeDescriptor> vars;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.vars = new HashMap<>();
    }

    public HashMap<String, TypeDescriptor> getVars() {

        HashMap<String, TypeDescriptor> newHashMap = new HashMap<>(this.vars);

        if (this.parentScope != null)
            newHashMap.putAll(this.parentScope.getVars());

        return newHashMap;
    }

    public void addVar(String localIdentifier, TypeDescriptor typeDescriptor) {
        this.vars.put(localIdentifier, typeDescriptor);
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
}
