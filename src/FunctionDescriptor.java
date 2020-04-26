import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionDescriptor {
    private String returnType;
    private String methodName;
    private final LinkedHashMap<String, TypeDescriptor> params;
    private final Scope scope;
    private boolean fromSuper;

    private final SimpleNode methodNode;

    public FunctionDescriptor(Scope parentScope, SimpleNode methodNode) {
        this.params = new LinkedHashMap<>();
        this.scope = new Scope(parentScope);
        this.methodNode = methodNode;
        this.fromSuper = false;
    }

    public String getIdentifier() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.methodName);

        for (Map.Entry<String, TypeDescriptor> entry : this.params.entrySet())
            stringBuilder.append(entry.getValue().getTypeIdentifier());

        return stringBuilder.toString();
    }

    public boolean isFromSuper() {
        return fromSuper;
    }

    public void setFromSuper(boolean fromSuper) {
        this.fromSuper = fromSuper;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public LinkedHashMap<String, TypeDescriptor> getParams() {
        return this.params;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void addParam(String identifier, TypeDescriptor typeDescriptor) {
        typeDescriptor.setInit(true);
        this.params.put(identifier, typeDescriptor);
    }

    public void addVar(String identifier, TypeDescriptor typeDescriptor) {
        this.scope.addVar(identifier, typeDescriptor);
    }

    public boolean isDeclared(String identifier) {
        return this.scope.getVars().containsKey(identifier) || this.params.containsKey(identifier);
    }

    public Scope getScope() {
        return scope;
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

    public SimpleNode getMethodNode() {
        return methodNode;
    }
}
