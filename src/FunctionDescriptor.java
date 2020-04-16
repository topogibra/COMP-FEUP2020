import java.util.HashMap;

public class FunctionDescriptor {
    private String returnType;
    private String identifier;
    private final HashMap<String, TypeDescriptor> params;
    private final Scope scope;

    public FunctionDescriptor(Scope parentScope) {
        this.params = new HashMap<>();
        this.scope = new Scope(parentScope);
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void addParam(String identifier, TypeDescriptor typeDescriptor) {
        this.params.put(identifier, typeDescriptor);
    }

    public void addVar(String identifier, TypeDescriptor typeDescriptor) {
        this.scope.addVar(identifier, typeDescriptor);
    }
}
