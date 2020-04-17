import java.util.HashMap;

public class Scope {
    private Scope parentScope;
    private HashMap<String, TypeDescriptor> vars;

    public Scope(Scope parentScope) {
        this.parentScope = parentScope;
        this.vars = new HashMap<>();
    }

    public HashMap<String, TypeDescriptor> getVars() {
        HashMap<String, TypeDescriptor> newHashMap = new HashMap<>(this.vars);

        /*if (this.parentScope != null)
            newHashMap.putAll(this.parentScope.getVars());*/

        return newHashMap;
    }

    public void addVar(String localIdentifier, TypeDescriptor typeDescriptor) {
        this.vars.put(localIdentifier, typeDescriptor);
    }
}
