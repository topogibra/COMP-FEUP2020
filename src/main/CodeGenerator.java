package main;

import Types.NodeName;
import Types.VarTypes;
import parser.Node;
import parser.SimpleNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class CodeGenerator {
    private final String DEST_DIRECTORY = "jasminCode/";
    private final String FILE_EXTENSION = ".j";
    private final String INDENTATION = "\t";

    private final SymbolTables symbolTables;
    private Path filePath;

    public CodeGenerator(SymbolTables symbolTables) {
        this.symbolTables = symbolTables;
    }

    public void generate() throws Exception {
        this.createFile(this.symbolTables.getClassName());

        this.generateClass();
        this.write("\n");
        this.generateFields();
        this.write("\n");
        this.generateConstructor();
        this.write("\n");
        this.generateMethods();
        this.write("\n");
    }

    private void createFile(String className) {

        Path directory = Paths.get(DEST_DIRECTORY);
        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        this.filePath = Paths.get(DEST_DIRECTORY + className + FILE_EXTENSION);
        try {
            if (!Files.exists(this.filePath))
                Files.createFile(this.filePath);
            else
                Files.write(this.filePath, "".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(String string) throws IOException {
        Files.write(this.filePath, string.getBytes(), StandardOpenOption.APPEND);
    }

    private void generateClass() throws IOException {
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

    private void generateFields() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, TypeDescriptor> field : symbolTables.getScope().getVars().entrySet()) {
            stringBuilder.append(".field public ");
            stringBuilder.append(field.getKey()).append(" ");
            stringBuilder.append(field.getValue().toJVM()).append("\n");
        }

        this.write(stringBuilder.toString());
    }

    private void generateConstructor() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(".method <init>()V\n");
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

    private void generateMethods() throws Exception {
        for (Map.Entry<String, FunctionDescriptor> method : symbolTables.getMethods().entrySet()) {
            this.generateMethod(method.getValue());
            this.write("\n");
        }
    }

    private void generateMethod(FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        AssemblerLabels assemblerLabels = new AssemblerLabels();

        if (functionDescriptor.getMethodNode() == null)
            return;

        stringBuilder.append(".method public ");

        //Method header
        if (functionDescriptor.getMethodName().equals("main"))
            stringBuilder.append("static main([Ljava/lang/String;)V");
        else {
            //Method parameters
            stringBuilder.append(this.generateMethodHeader(functionDescriptor));
        }

        stringBuilder.append("\n");

        //### Method body ####
        stringBuilder.append(INDENTATION).append(".limit stack 99\n");
        stringBuilder.append(INDENTATION).append(".limit locals 99\n");
        stringBuilder.append("\n");

        //Method Body
        stringBuilder.append(this.generateMethodBody(functionDescriptor, assemblerLabels));

        // Return expression
        if (functionDescriptor.getMethodName().equals("main"))
            stringBuilder.append(this.generateReturn(functionDescriptor, assemblerLabels));

        stringBuilder.append(".end method\n");

        this.write(stringBuilder.toString());
    }

    private String generateMethodBody(FunctionDescriptor functionDescriptor, AssemblerLabels assemblerLabels) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        SimpleNode methodBody = null;

        // Look for method body node
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
                case NodeName.DOTMETHOD:
                    stringBuilder.append(this.generateDotMethod(functionDescriptor, child, assemblerLabels)).append("\n");
                    break;
                case NodeName.ASSIGNMENT:
                    stringBuilder.append(this.generateAssignment(child, functionDescriptor, assemblerLabels));
                    break;
                case NodeName.RETURN:
                    stringBuilder.append(this.generateReturn(functionDescriptor, child, assemblerLabels));
                    break;
                default:
                    break;
            }
        }

        return stringBuilder.toString();
    }

    private String generateDotMethod(FunctionDescriptor functionDescriptor, SimpleNode dotMethodNode, AssemblerLabels assemblerLabels) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        SimpleNode leftSide = (SimpleNode) dotMethodNode.jjtGetChild(0);
        SimpleNode rightSide = (SimpleNode) dotMethodNode.jjtGetChild(1);

        if (Utils.isClassVariable(this.symbolTables, leftSide, functionDescriptor)) {
            if(leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
                TypeDescriptor classDescriptor = functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal()); //TODO only workds if left side is identifier
                stringBuilder.append(INDENTATION).append("aload ").append(classDescriptor.getIndex()).append("\n");
            }

            if (rightSide.jjtGetNumChildren() > 1) // If arguments are being passed
                stringBuilder.append(this.generateArgumentsLoading(functionDescriptor, (SimpleNode) rightSide.jjtGetChild(1), assemblerLabels));

            String methodId = Utils.parseMethodIdentifier(this.symbolTables, rightSide, functionDescriptor);
            FunctionDescriptor descriptor = symbolTables.getFunctionDescriptor(methodId);

            stringBuilder.append(INDENTATION);
            stringBuilder.append((descriptor.isFromSuper()) ? "invokespecial " : "invokevirtual ");
            stringBuilder.append((descriptor.isFromSuper()) ? symbolTables.getExtendedClassName() : symbolTables.getClassName());
            stringBuilder.append("/").append(this.generateMethodHeader(descriptor)).append("\n");
        } else {
            ImportDescriptor importDescriptor = Utils.getImportedMethod(this.symbolTables, dotMethodNode, functionDescriptor);
            if (importDescriptor != null) { // Invoke imported method

                if (rightSide.jjtGetNumChildren() > 1) // If arguments are being passed
                    stringBuilder.append(this.generateArgumentsLoading(functionDescriptor, (SimpleNode) rightSide.jjtGetChild(1), assemblerLabels));

                if (importDescriptor.isStatic())
                    stringBuilder.append(INDENTATION).append("invokestatic ");
                else
                    stringBuilder.append(INDENTATION).append("invokevirtual ");
                stringBuilder.append(this.generateMethodHeader(importDescriptor)).append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private String parseBoolean(SimpleNode simpleNode) {
        return simpleNode.jjtGetVal().equals("true") ? "iconst_1" : "iconst_0";
    }

    private String generateAssignment(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, AssemblerLabels assemblerLabels) throws Exception {
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
                stringBuilder.append(this.generateDotMethod(functionDescriptor, rightSide, assemblerLabels));
                break;
            }
            default: {
                if (Utils.isArithmeticExpression(rightSide))
                    stringBuilder.append(this.generateArithmeticExpression(rightSide, functionDescriptor, assemblerLabels));
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

    private String generateMethodHeader(FunctionDescriptor descriptor){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(descriptor.getMethodName());

        stringBuilder.append("(");
        for (Map.Entry<String, TypeDescriptor> param : descriptor.getParams().entrySet())
            stringBuilder.append(param.getValue().toJVM());
        stringBuilder.append(")");

        stringBuilder.append(TypeDescriptor.toJVM(descriptor.getReturnType()));
        return  stringBuilder.toString();
    }

    private String generateMethodHeader(ImportDescriptor descriptor){
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

    private String generateArithmeticExpression(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, AssemblerLabels assemblerLabels) throws Exception {
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
                case NodeName.IDENTIFIER: {
                    TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                    stringBuilder.append(this.parseTypeDescriptorLoader(typeDescriptor)).append("\n");
                    break;
                }
                case NodeName.DOTMETHOD: {
                    stringBuilder.append(this.generateDotMethod(functionDescriptor, simpleNode, assemblerLabels));
                    break;
                }
            }

            return stringBuilder.toString();
        }


        for (Node nodeChild : children) {
            SimpleNode child = (SimpleNode) nodeChild;

            if (child != null)
                stringBuilder.append(generateArithmeticExpression(child, functionDescriptor, assemblerLabels));
        }

        // Operations
        switch (nodeName) {
            case NodeName.ADD:
            case NodeName.SUB:
            case NodeName.DIV:
            case NodeName.MUL: {
                stringBuilder.append(INDENTATION).append("i").append(nodeName.toLowerCase()).append("\n");
                break;
            }
            case NodeName.NOT: {
                final String label = assemblerLabels.getLabel("put_true");
                stringBuilder.append(INDENTATION).append("iconst_0\n"); // Push 0 (false) to stack
                stringBuilder.append(INDENTATION).append("if_icmpeq ").append(label).append("\n"); // Compare previous top value with 0: if previous_value == false
                                                                                // If yes, goto put_true, to put 1 in top of stack
                stringBuilder.append(INDENTATION).append("iconst_0\n");
                stringBuilder.append(label).append(":\n");
                stringBuilder.append(INDENTATION).append("iconst_1\n");
                break;
            }
            case NodeName.AND: {
                String putFalseLabel = assemblerLabels.getLabel("put_false");
                String putFalseAndPopLabel = assemblerLabels.getLabel("put_false_&_pop");
                String endAndLabel = assemblerLabels.getLabel("end_and");

                stringBuilder.append(INDENTATION).append("iconst_0\n");
                stringBuilder.append(INDENTATION).append("if_icmpeq ").append(putFalseAndPopLabel).append("\n");
                stringBuilder.append(INDENTATION).append("iconst_0\n");
                stringBuilder.append(INDENTATION).append("if_icmpeq ").append(putFalseLabel).append("\n");
                stringBuilder.append(INDENTATION).append("iconst_1\n");
                stringBuilder.append(INDENTATION).append("goto ").append(endAndLabel).append("\n");
                stringBuilder.append(putFalseAndPopLabel).append(":\n");
                stringBuilder.append(INDENTATION).append("pop\n");
                stringBuilder.append(putFalseLabel).append(":\n");
                stringBuilder.append(INDENTATION).append("iconst_0\n");
                stringBuilder.append(endAndLabel).append(":\n");
                break;
            }
            case NodeName.LESS: {
                //TODO
                break;
            }
        }

        return stringBuilder.toString();
    }

    public String generateArgumentsLoading(FunctionDescriptor functionDescriptor, SimpleNode argsNode, AssemblerLabels assemblerLabels) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        Node[] children = argsNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode arg = (SimpleNode) node;

            switch (arg.getNodeName()) {
                case NodeName.IDENTIFIER: {
                    TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(arg.jjtGetVal());
                    stringBuilder.append(parseTypeDescriptorLoader(typeDescriptor));
                    stringBuilder.append("\n");
                    break;
                }
                case NodeName.DOTMETHOD: {
                    stringBuilder.append(this.generateDotMethod(functionDescriptor, arg, assemblerLabels));
                    break;
                }
            }
        }

        return stringBuilder.toString();
    }

    private String generateReturn(FunctionDescriptor functionDescriptor, AssemblerLabels assemblerLabels) throws Exception {
        return this.generateReturn(functionDescriptor, null, assemblerLabels);
    }

    private String generateReturn(FunctionDescriptor functionDescriptor, SimpleNode returnNode, AssemblerLabels assemblerLabels) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        if (returnNode != null && returnNode.jjtGetNumChildren() > 0) {
            SimpleNode child = returnNode.getChild(0);

            // return EXPRESSION;
            switch (child.getNodeName()) {
                case NodeName.IDENTIFIER: {
                    TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(child.jjtGetVal());
                    stringBuilder.append(this.parseTypeDescriptorLoader(typeDescriptor)).append("\n");
                    break;
                }
                case NodeName.INT: {
                    stringBuilder.append(INDENTATION).append("bipush ").append(child.jjtGetVal()).append("\n");
                    break;
                }
                case NodeName.BOOLEAN: {
                    stringBuilder.append(INDENTATION).append(this.parseBoolean(child)).append("\n");
                    break;
                }
                default: {
                    if (Utils.isArithmeticExpression(child))
                        stringBuilder.append(this.generateArithmeticExpression(child, functionDescriptor, assemblerLabels));
                }
            }

            stringBuilder.append("\n");
        }


        switch (functionDescriptor.getReturnType()) {
            case VarTypes.INT:
            case VarTypes.BOOLEAN: {
                stringBuilder.append(INDENTATION).append("ireturn\n");
                break;
            }
            case VarTypes.INTARRAY: {
                stringBuilder.append(INDENTATION).append("areturn\n");
                break;
            }
            case VarTypes.VOID: {
                stringBuilder.append(INDENTATION).append("return\n");
                break;
            }
            default: {
                if (functionDescriptor.getReturnType().equals(symbolTables.getClassName()))
                    stringBuilder.append(INDENTATION).append("areturn\n");
                break;
            }
        }

        return stringBuilder.toString();
    }

    private String parseTypeDescriptorLoader(TypeDescriptor typeDescriptor) {
        switch (typeDescriptor.getTypeIdentifier()) {
            case VarTypes.INT:
            case VarTypes.BOOLEAN: {
               return INDENTATION + "iload " + typeDescriptor.getIndex();
            }
            case VarTypes.INTARRAY: {
                //TODO
                break;
            }
            default: {
                if (symbolTables.getClassName().equals(typeDescriptor.getTypeIdentifier()))
                    return INDENTATION + "aload " + typeDescriptor.getIndex();
                break;
            }
        }
        return null;
    }

}

