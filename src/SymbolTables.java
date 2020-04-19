import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTables {
    private String className;
    private final LinkedHashMap<String, ImportDescriptor> imports;
    private final LinkedHashMap<String, FunctionDescriptor> methods;
    private final Scope scope;

    public SymbolTables() {
        this.imports = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
        this.scope = new Scope(null);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void addImport(ImportDescriptor importDescriptor) {
        this.imports.put(importDescriptor.getIdentifier(), importDescriptor);
    }

    public void addMethod(FunctionDescriptor functionDescriptor) {
        this.methods.put(functionDescriptor.getIdentifier(), functionDescriptor);
    }

    public Scope getScope() {
        return scope;
    }

    public void addVar(String identifier, TypeDescriptor typeDescriptor) {
        this.scope.addVar(identifier, typeDescriptor);
    }

    public FunctionDescriptor getFunctionDescriptor(String methodIdentifier) {
        return this.methods.get(methodIdentifier);
    }

    public void print() {
        for (Map.Entry<String, FunctionDescriptor> entry : this.methods.entrySet()) {
            System.out.println("Saved methods: " + entry.getKey());
            entry.getValue().printScope();
        }


        System.out.print("\n\n");
    }
}
