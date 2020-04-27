import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;


public class CodeGenerator {
    private SymbolTables symbolTables;
    private Path path;
    private final String FILE_EXTENSION = ".j";
    private final String INDENTATION = "\t";


    public CodeGenerator(SymbolTables symbolTables) {
        this.symbolTables = symbolTables;
    }

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

    public void generate(SimpleNode rootNode) throws Exception {
        this.createFile(symbolTables.getClassName());

        this.generateClass();
        this.write("\n");
        this.generateFields();
        this.write("\n");
        this.generateConstructor();
        this.write("\n");
        this.generateMethods();
        this.write("\n");

    }

    public void write(String string) throws IOException {
        Files.write(this.path, string.getBytes(), StandardOpenOption.APPEND);
    }

    public void generateClass() throws IOException {
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

    public void generateFields() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, TypeDescriptor> field : symbolTables.getScope().getVars().entrySet()) {
            stringBuilder.append(".field public ");
            stringBuilder.append(field.getKey()).append(" ");
            stringBuilder.append(field.getValue().toJVM()).append("\n");
        }

        this.write(stringBuilder.toString());
    }

    public void generateConstructor() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(".method public<init>()V\n");
        stringBuilder.append(INDENTATION).append("aload_0\n");
        stringBuilder.append(INDENTATION).append("invokenonvirtual ");

        if (symbolTables.getExtendedClassName() == null)
            stringBuilder.append("java/lang/Object/<init>()V\n");
        else
            stringBuilder.append(symbolTables.getExtendedClassName()).append("/<init>()V\n");

        stringBuilder.append(INDENTATION).append("return\n");
        stringBuilder.append(".end method\n");

        this.write(stringBuilder.toString());
    }

    public void generateMethods() throws Exception {
        for (Map.Entry<String, FunctionDescriptor> method : symbolTables.getMethods().entrySet()) {
            this.generateMethod(method.getValue());
            this.write("\n");
        }
    }

    public void generateMethod(FunctionDescriptor functionDescriptor) throws Exception {
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
        stringBuilder.append(INDENTATION).append(".limit stack 99\n");
        stringBuilder.append(INDENTATION).append(".limit locals 99\n");
        stringBuilder.append("\n");

        //Method Body
        stringBuilder.append(generateMethodBody(functionDescriptor));


        stringBuilder.append(".end method\n");
        this.write(stringBuilder.toString());
    }

    private String generateMethodBody(FunctionDescriptor functionDescriptor) throws Exception {
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
                    stringBuilder.append(this.generateDotMethod(functionDescriptor,child)).append("\n");
                    break;
                }
                case NodeName.ASSIGNMENT: {
                    stringBuilder.append(this.generateAssignment(child, functionDescriptor));
                    break;
                }
                default:
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private String generateDotMethod(FunctionDescriptor functionDescriptor, SimpleNode dotMethodNode) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        SimpleNode leftSide = (SimpleNode) dotMethodNode.jjtGetChild(0);
        SimpleNode rightSide = (SimpleNode) dotMethodNode.jjtGetChild(1);

        String methodId = SemanticAnalyser.parseMethodIdentifier(symbolTables,rightSide,functionDescriptor);
        FunctionDescriptor descriptor = symbolTables.getFunctionDescriptor(methodId);

        if (rightSide.jjtGetNumChildren() > 1)
            stringBuilder.append(this.generateArgumentsLoading(functionDescriptor, rightSide));

        if (SemanticAnalyser.isClassVariable(symbolTables,leftSide,functionDescriptor)) {
            stringBuilder.append(INDENTATION);
            stringBuilder.append((descriptor.isFromSuper()) ? ".invokespecial " : ".invokevirtual ");
            stringBuilder.append((descriptor.isFromSuper()) ? symbolTables.getExtendedClassName() : symbolTables.getClassName());
            stringBuilder.append("/").append(printMethod(descriptor)).append("\n");
        } else {
            ImportDescriptor importDescriptor = SemanticAnalyser.getImportedMethod(symbolTables,dotMethodNode,functionDescriptor);
            if (importDescriptor != null) { // Invoke imported method
                stringBuilder.append(INDENTATION).append(".invokestatic "); //TODO: only invoke static if imported static
                stringBuilder.append(printMethod(importDescriptor)).append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private String parseBoolean(SimpleNode simpleNode) {
        return simpleNode.jjtGetVal().equals("true") ? "iconst_1" : "iconst_0";
    }

    private String generateAssignment(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        SimpleNode leftSide = (SimpleNode) simpleNode.jjtGetChild(0);
        SimpleNode rightSide = (SimpleNode) simpleNode.jjtGetChild(1);

        switch (rightSide.getNodeName()) {
            case NodeName.INT: {
                stringBuilder.append(INDENTATION).append("bipush ").append(rightSide.jjtGetVal()).append("\n");
                break;
            }
            case NodeName.BOOLEAN: {
                stringBuilder.append(INDENTATION).append(this.parseBoolean(rightSide)).append("\n");
                break;
            }
            case NodeName.IDENTIFIER: {

                break;
            }
            case NodeName.NEW : {
                SimpleNode identifierChild = (SimpleNode) rightSide.jjtGetChild(0);
                stringBuilder.append(INDENTATION).append("new ").append(identifierChild.jjtGetVal()).append("\n");
                stringBuilder.append(INDENTATION).append("dup\n");
                stringBuilder.append(INDENTATION).append("invokespecial ").append(identifierChild.jjtGetVal()).append("/<init>()V\n");
                break;
            }
            case NodeName.DOTMETHOD: {
                stringBuilder.append(this.generateDotMethod(functionDescriptor, rightSide));
                break;
            }
            default: {
                if (SemanticAnalyser.isExpression(rightSide))
                    stringBuilder.append(this.generateArithmeticExpression(rightSide, functionDescriptor));
                break;
            }
        }

        if (leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
            String typeIdentifier = typeDescriptor.getTypeIdentifier();

            switch (typeIdentifier) {
                case VarTypes.INT:
                case VarTypes.BOOLEAN: {
                    stringBuilder.append(INDENTATION).append("istore ").append(typeDescriptor.getIndex()).append("\n");
                    break;
                }
                default: {
                    if (symbolTables.getClassName().equals(typeIdentifier))
                    stringBuilder.append(INDENTATION).append("astore ").append(typeDescriptor.getIndex()).append("\n");
                    break;
                }

            }

        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String printMethod(FunctionDescriptor descriptor){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(descriptor.getMethodName());

        stringBuilder.append("(");
        for (Map.Entry<String, TypeDescriptor> param : descriptor.getParams().entrySet())
            stringBuilder.append(param.getValue().toJVM());
        stringBuilder.append(")");

        stringBuilder.append(TypeDescriptor.toJVM(descriptor.getReturnType()));
        return  stringBuilder.toString();
    }

    private String printMethod(ImportDescriptor descriptor){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(descriptor.getClassName()).append("/");
        stringBuilder.append(descriptor.getMethodName());

        stringBuilder.append("(");
        for (TypeDescriptor param : descriptor.getArguments())
            stringBuilder.append(param.toJVM());
        stringBuilder.append(")");

        stringBuilder.append(descriptor.getReturnType().toJVM());
        return  stringBuilder.toString();
    }

    private String generateArithmeticExpression(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();

        String nodeName = simpleNode.getNodeName();

        Node[] children = simpleNode.jjtGetChildren();

        // Primitives
        if (children == null || children.length == 0) {
            switch (nodeName) {
                case NodeName.INT: {
                    stringBuilder.append(INDENTATION).append("bipush ").append(simpleNode.jjtGetVal()).append("\n");
                    break;
                }
                case NodeName.BOOLEAN: {
                    stringBuilder.append(INDENTATION);
                    String val = this.parseBoolean(simpleNode);
                    stringBuilder.append(val).append("\n");
                    break;
                }
            }

            return stringBuilder.toString();
        }

        for (int i = simpleNode.jjtGetNumChildren() - 1; i >= 0; i--) {
            SimpleNode child = (SimpleNode) simpleNode.jjtGetChild(i);

            if (child != null)
                stringBuilder.append(generateArithmeticExpression(child, functionDescriptor));
        }

        stringBuilder.append(INDENTATION).append("i").append(nodeName.toLowerCase()).append("\n");

        // Operations
        /*
        switch (nodeName) {
            case NodeName.ADD: {
                stringBuilder.append(INDENTATION).append("iadd\n");
                break;
            }
            case NodeName.SUB: {
                stringBuilder.append(INDENTATION).append("isub\n");
                break;
            }
            case NodeName.DIV: {
                stringBuilder.append(INDENTATION).append("idiv\n");
                break;
            }
            case NodeName.MUL: {
                stringBuilder.append(INDENTATION).append("imul\n");
                break;
            }
        }*/

        return stringBuilder.toString();
    }

    public String generateArgumentsLoading(FunctionDescriptor functionDescriptor, SimpleNode methodCallNode) {
        StringBuilder stringBuilder = new StringBuilder();

        SimpleNode methodNameNode = (SimpleNode) methodCallNode.jjtGetChild(0);
        SimpleNode argsNode = (SimpleNode) methodCallNode.jjtGetChild(1);

        for (int i = argsNode.jjtGetNumChildren() - 1; i >= 0; i--) {
            SimpleNode arg = (SimpleNode) argsNode.jjtGetChild(i);

            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(arg.jjtGetVal());

            switch (typeDescriptor.getTypeIdentifier()) {
                case VarTypes.INT:
                case VarTypes.BOOLEAN: {
                    stringBuilder.append(INDENTATION).append("iload ").append(typeDescriptor.getIndex());
                    break;
                }
                case VarTypes.INTARRAY: {
                    //TODO
                    break;
                }
                default: {
                    if (symbolTables.getClassName().equals(typeDescriptor.getTypeIdentifier()))
                        stringBuilder.append(INDENTATION).append("aload ").append(typeDescriptor.getIndex());
                    break;
                }
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

}

