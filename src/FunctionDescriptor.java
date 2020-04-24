import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionDescriptor {
    private String returnType;
    private String methodName;
    private final LinkedHashMap<String, TypeDescriptor> params;
    private final Scope scope;

    public FunctionDescriptor(Scope parentScope) {
        this.params = new LinkedHashMap<>();
        this.scope = new Scope(parentScope);
    }

    public String getIdentifier() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.methodName);

        for (Map.Entry<String, TypeDescriptor> entry : this.params.entrySet())
            stringBuilder.append(entry.getValue().getTypeIdentifier());

        return stringBuilder.toString();
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void addParam(String identifier, TypeDescriptor typeDescriptor) {
        this.params.put(identifier, typeDescriptor);
    }

    public void addVar(String identifier, TypeDescriptor typeDescriptor) {
        this.scope.addVar(identifier, typeDescriptor);
    }

    public boolean isDeclared(String identifier) {
        return this.scope.getVars().containsKey(identifier) || this.params.containsKey(identifier);
    }

    public TypeDescriptor getTypeDescriptor(String identifier) {
        TypeDescriptor typeDescriptor = this.scope.getVars().get(identifier);

        if (typeDescriptor == null)
            typeDescriptor = this.params.get(identifier);

        return typeDescriptor;
    }

    public void printScope() {
        this.scope.printScopeVars();
    }
}
