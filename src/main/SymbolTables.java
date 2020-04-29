package main;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolTables {
    private String className;
    private String extendedClassName;
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

    public ImportDescriptor getImportDescriptor(String importedMethodIdentifier) {
        return this.imports.get(importedMethodIdentifier);
    }

    public LinkedHashMap<String, FunctionDescriptor> getMethods() {
        return this.methods;
    }

    public void print() {

        System.out.print("\n\n");

        for (Map.Entry<String, FunctionDescriptor> entry : this.methods.entrySet()) {
            System.out.println("Saved methods: " + entry.getKey());
            //entry.getValue().printScope();
        }

        System.out.print("\n\n");

        for (Map.Entry<String, ImportDescriptor> entry : this.imports.entrySet()) {
            System.out.println("Saved import: " + entry.getKey());
        }


        System.out.print("\n\n");
    }

    public void setExtendedClass(String extendedClassName) {
        this.extendedClassName = extendedClassName;

        for (Map.Entry<String, ImportDescriptor> entry : this.imports.entrySet()) {
            ImportDescriptor importDescriptor = entry.getValue();
            if (importDescriptor.getClassName().equals(extendedClassName) && importDescriptor.getMethodName() != null) {
                FunctionDescriptor functionDescriptor = new FunctionDescriptor(this.scope, null);
                functionDescriptor.setFromSuper(true);
                functionDescriptor.setMethodName(importDescriptor.getMethodName());
                functionDescriptor.setReturnType(importDescriptor.getReturnType().getTypeIdentifier());

                int id = 0;
                for (TypeDescriptor typeDescriptor : importDescriptor.getArguments()) {
                    functionDescriptor.addParam(Integer.toString(id), typeDescriptor);
                    id++;
                }

                this.methods.put(functionDescriptor.getIdentifier(), functionDescriptor);
            }
        }
    }

    public boolean isImportedClass(String extendedClassName) {
        return this.imports.containsKey(extendedClassName);
    }

    public String getClassName() {
        return className;
    }

    public String getExtendedClassName() {
        return extendedClassName;
    }

    public void setExtendedClassName(String extendedClassName) {
        this.extendedClassName = extendedClassName;
    }
}
