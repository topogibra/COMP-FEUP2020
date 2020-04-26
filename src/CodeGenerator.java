import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;


public class CodeGenerator {
    private Path path;
    private final String FILE_EXTENSION = ".j";
    private final String IDENTATION = "\t";

    private void createFile(String className) {
        this.path = Paths.get(className + FILE_EXTENSION);

        try {
            if (!Files.exists(this.path))
                Files.createFile(this.path);
            else
                Files.write(this.path, "".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generate(SymbolTables symbolTables, SimpleNode rootNode) throws Exception {
        this.createFile(symbolTables.getClassName());

        this.generateClass(symbolTables);
        this.write("\n");
        this.generateFields(symbolTables);
        this.write("\n");
        this.generateMethods(symbolTables);
        this.write("\n");

    }

    public void write(String string) throws IOException {
        Files.write(this.path, string.getBytes(), StandardOpenOption.APPEND);
    }

    public void generateClass(SymbolTables symbolTables) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(".class public ").append(symbolTables.getClassName()).append("\n");

        stringBuilder.append(".super ");
        if (symbolTables.getExtendedClassName() != null)
            stringBuilder.append(symbolTables.getExtendedClassName());
        else
            stringBuilder.append("java/lang/Object");

        stringBuilder.append("\n");

        this.write(stringBuilder.toString());
    }

    public void generateFields(SymbolTables symbolTables) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, TypeDescriptor> field : symbolTables.getScope().getVars().entrySet()) {
            stringBuilder.append(".field public ");
            stringBuilder.append(field.getKey()).append(" ");
            stringBuilder.append(field.getValue().toJVM()).append("\n");
        }

        this.write(stringBuilder.toString());
    }

    public void generateMethods(SymbolTables symbolTables) throws Exception {
        for (Map.Entry<String, FunctionDescriptor> method : symbolTables.getMethods().entrySet()) {
            this.generateMethod(symbolTables, method.getValue());
            this.write("\n");
        }
    }

    public void generateMethod(SymbolTables symbolTables, FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        if (functionDescriptor.getMethodNode() == null)
            return;

        stringBuilder.append(".method public ");

        //Method header
        if (functionDescriptor.getMethodName().equals("main"))
            stringBuilder.append("static main([Ljava/lang/String;)V");
        else {
            //Method parameters
            stringBuilder.append(printMethod(functionDescriptor));
        }

        stringBuilder.append("\n");

        //### Method body ####
        //Var declarations
        stringBuilder.append(IDENTATION).append(".limit stack 99\n");
        stringBuilder.append(IDENTATION).append(".limit locals 99\n");
        stringBuilder.append("\n");

        //Method Body
        stringBuilder.append(generateMethodBody(symbolTables,functionDescriptor));


        stringBuilder.append(".end method\n");
        this.write(stringBuilder.toString());
    }

    private String generateMethodBody(SymbolTables symbolTables, FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        SimpleNode methodBody = null;

        for (Node node: functionDescriptor.getMethodNode().jjtGetChildren()) {
            SimpleNode child = (SimpleNode) node;

            if(child.getNodeName().equals(NodeName.METHODBODY)){
                methodBody = child;
                break;
            }

        }

        if (methodBody == null){
            throw new Exception("Couldn't find methodBody node");
        }

        for(Node node: methodBody.jjtGetChildren()){
            SimpleNode child = (SimpleNode) node;

            switch (child.getNodeName()){
                case NodeName.DOTMETHOD:{
                    stringBuilder.append(IDENTATION);
                    stringBuilder.append(generateDotMethod(symbolTables,functionDescriptor,child)).append("\n");
                    break;
                }
                default:
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private String generateDotMethod(SymbolTables symbolTables, FunctionDescriptor functionDescriptor, SimpleNode dotMethodNode) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        SimpleNode leftSide = (SimpleNode) dotMethodNode.jjtGetChild(0);
        SimpleNode rightSide = (SimpleNode) dotMethodNode.jjtGetChild(1);

        String methodId = SemanticAnalyser.parseMethodIdentifier(symbolTables,rightSide,functionDescriptor);
        FunctionDescriptor descriptor = symbolTables.getFunctionDescriptor(methodId);

        if(SemanticAnalyser.isClassVariable(symbolTables,leftSide,functionDescriptor)){
            stringBuilder.append((descriptor.isFromSuper()) ? ".invokespecial " : ".invokevirtual ");
            stringBuilder.append((descriptor.isFromSuper()) ? symbolTables.getExtendedClassName() : symbolTables.getClassName());
            stringBuilder.append("/").append(printMethod(descriptor)).append("\n");
        } else {
            ImportDescriptor importDescriptor = SemanticAnalyser.getImportedMethod(symbolTables,dotMethodNode,functionDescriptor);
            if (importDescriptor != null) { // Invoke imported method
                stringBuilder.append(".invokestatic ");
                stringBuilder.append(printMethod(importDescriptor)).append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private String printMethod(FunctionDescriptor descriptor){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(descriptor.getMethodName());
        stringBuilder.append("(");
        if (descriptor.getParams().size() > 0) {
            for (Map.Entry<String, TypeDescriptor> param : descriptor.getParams().entrySet()) {
                stringBuilder.append(param.getValue().toJVM());
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        stringBuilder.append(")");
        stringBuilder.append(TypeDescriptor.toJVM(descriptor.getReturnType()));
        return  stringBuilder.toString();
    }

    private String printMethod(ImportDescriptor descriptor){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(descriptor.getClassName()).append("/");
        stringBuilder.append(descriptor.getMethodName());
        stringBuilder.append("(");
        if (descriptor.getArguments().size() > 0) {
            for (TypeDescriptor param : descriptor.getArguments()) {
                stringBuilder.append(param.toJVM());
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);
        }
        stringBuilder.append(")");
        stringBuilder.append(descriptor.getReturnType().toJVM());
        return  stringBuilder.toString();
    }
}

