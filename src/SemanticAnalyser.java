
public class SemanticAnalyser {
    private static final int MAX_NUM_ERRORS = 10;
    private int num_errors = 0;

    public static void analyse(SymbolTables symbolTables, SimpleNode simpleNode) throws Exception {

        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];
        switch (nodeName) {
            case NodeName.METHOD: {
                String methodIdentifier = getMethodIdentifier(simpleNode);
                System.out.print("Method id: " + methodIdentifier + "\n");
                analyseMethod(symbolTables, simpleNode, symbolTables.getFunctionDescriptor(methodIdentifier));
                break;
            }
            case NodeName.EXTENDS: {
                String extendedClassName = ((SimpleNode) simpleNode.jjtGetChild(0)).jjtGetVal();
                if (!symbolTables.isImportedClass(extendedClassName))
                    throw new SemanticException(simpleNode);
                break;
            }
        }

        Node[] children = simpleNode.jjtGetChildren();
        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                analyse(symbolTables, child);
            }
        }
    }

    public static void analyseMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = simpleNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                analyseMethod(symbolTables, child, functionDescriptor);
            }
        }

        switch (ParserTreeConstants.jjtNodeName[simpleNode.getId()]) {
            case NodeName.ARRAYACCESS:
                if (!analyseArray(false, symbolTables, simpleNode, functionDescriptor))
                    throw new SemanticException(simpleNode);
                break;
            case NodeName.ARRAYSIZE:
                if (!analyseArray(true, symbolTables, simpleNode, functionDescriptor))
                    throw new SemanticException(simpleNode);
                break;
            case NodeName.DOTMETHOD:
                if (analyseDotMethod(symbolTables, simpleNode, functionDescriptor) == null)
                    throw new SemanticException(simpleNode);
                break;
            default:
                break;
        }
    }

    public static String getMethodIdentifier(SimpleNode simpleNode) {
        StringBuilder stringBuilder = new StringBuilder();

        Node[] children = simpleNode.jjtGetChildren();

        stringBuilder.append(((SimpleNode) children[1]).jjtGetVal()); // Method Name

        if (!ParserTreeConstants.jjtNodeName[((SimpleNode) children[2]).getId()].equals(NodeName.ARGS))
            return stringBuilder.toString();

        Node[] grandchildren = ((SimpleNode) children[2]).jjtGetChildren();
        for (Node node : grandchildren) {
            SimpleNode simpleNode1 = (SimpleNode) node;
            stringBuilder.append(((SimpleNode) simpleNode1.jjtGetChildren()[0]).jjtGetVal());
        }

        return stringBuilder.toString();
    }

    public static boolean analyseArray(boolean isArraySize, SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];

        if (!isArraySize) { //Check if it is an array that's being accessed
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(firstChild.jjtGetVal());
            if (!typeDescriptor.isArray())
                return false;
        }

        SimpleNode analysedChild = (SimpleNode) (isArraySize ? firstChild : simpleNode.jjtGetChildren()[1]);
        String nodeName = ParserTreeConstants.jjtNodeName[analysedChild.getId()];

        System.out.print("NodeName: " + nodeName + "\n");
        System.out.print("NodeValue: " + analysedChild.jjtGetVal() + "\n");

        // Analyses if value inside array access is integer
        return isInteger(symbolTables, analysedChild, functionDescriptor);
    }

    private static String analyseDotMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String secondChildName = ParserTreeConstants.jjtNodeName[secondChild.getId()];

        if (isClassVariable(symbolTables, firstChild, functionDescriptor)) {
            if (secondChildName.equals(NodeName.METHODCALL))
                return getMethodReturnType(symbolTables, secondChild, functionDescriptor);
        } else {
            ImportDescriptor importDescriptor = getImportedMethod(symbolTables, simpleNode, functionDescriptor);
            if (importDescriptor != null)
                return importDescriptor.getReturnType().getTypeIdentifier();
            else if (secondChildName.equals(NodeName.LENGTH))
                return "int";
        }

        return null;
    }

    private static boolean isClassVariable(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        switch (nodeName) {
            case NodeName.THIS:
                return true;
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (typeDescriptor != null) {
                    String typeIdentifier = typeDescriptor.getTypeIdentifier();
                    System.out.println("Type identifier: " + typeIdentifier);
                    return typeIdentifier.equals(symbolTables.getClassName());
                }
                return false;
            }
            case NodeName.NEW: {
                SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
                return firstChild.jjtGetVal().equals(symbolTables.getClassName());
            }
        }

        return false;
    }

    private static String getMethodReturnType(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        String methodIdentifier = parseMethodIdentifier(symbolTables, simpleNode, functionDescriptor);
        FunctionDescriptor methodDescriptor = symbolTables.getFunctionDescriptor(methodIdentifier);
        return (methodDescriptor == null) ? null : methodDescriptor.getReturnType();
    }

    private static String parseMethodIdentifier(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        StringBuilder methodIdentifier = new StringBuilder();

        if (simpleNode.jjtGetChildren() == null || simpleNode.jjtGetChildren().length == 0)
            return "";

        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        if (ParserTreeConstants.jjtNodeName[firstChild.getId()].equals(NodeName.METHODNAME))
            methodIdentifier.append(firstChild.jjtGetVal());
        else
            throw new SemanticException(simpleNode);

        if (simpleNode.jjtGetChildren().length > 1) { //Check if method call has arguments

            SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];
            if (ParserTreeConstants.jjtNodeName[secondChild.getId()].equals(NodeName.ARGS)) {

                for (Node grandchildNode : secondChild.jjtGetChildren()) {
                    SimpleNode grandChild = (SimpleNode) grandchildNode;

                    String nodeName = ParserTreeConstants.jjtNodeName[grandChild.getId()];
                    if (isExpression(grandChild)) {
                        System.out.print("is expression\n");
                        System.out.print("Nodename: " + nodeName + "\n");
                        String returnType = analyseExpression(symbolTables, grandChild, functionDescriptor);
                        methodIdentifier.append(returnType);

                        continue;
                    }

                    switch (nodeName) {
                        case NodeName.IDENTIFIER: {
                            String argIdentifier = grandChild.jjtGetVal();
                            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(argIdentifier);
                            if (typeDescriptor != null)
                                methodIdentifier.append(typeDescriptor.getTypeIdentifier());
                            else
                                throw new SemanticException(grandChild);
                            break;
                        }
                        case NodeName.DOTMETHOD: {
                            String returnType = analyseDotMethod(symbolTables, grandChild, functionDescriptor);
                            if (returnType != null)
                                methodIdentifier.append(returnType);
                            else
                                throw new SemanticException(grandChild);
                            break;
                        }
                        case NodeName.ARRAYACCESS: {
                            if (analyseArray(false, symbolTables, grandChild, functionDescriptor)) {
                                methodIdentifier.append("int");
                            }
                            break;
                        }
                        case NodeName.INT : {
                            methodIdentifier.append("int");
                            break;
                        }
                        case NodeName.BOOLEAN : {
                            methodIdentifier.append("boolean");
                            break;
                        }
                        default:
                            throw new SemanticException(grandChild);
                    }
                }
            } else
                throw new SemanticException(simpleNode);
        }

        return methodIdentifier.toString();
    }

    private static ImportDescriptor getImportedMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String importedMethodIdentifier = firstChild.jjtGetVal();
        importedMethodIdentifier += parseMethodIdentifier(symbolTables, secondChild, functionDescriptor);

        return symbolTables.getImportDescriptor(importedMethodIdentifier);
    }

    private static boolean isExpression(SimpleNode simpleNode) {
        switch (ParserTreeConstants.jjtNodeName[simpleNode.getId()]) {
            case NodeName.ADD:
            case NodeName.SUB:
            case NodeName.MUL:
            case NodeName.DIV:
            case NodeName.AND:
            case NodeName.LESS:
            case NodeName.NOT:
                return true;
            default:
                return false;
        }
    }

    private static String analyseExpression(SymbolTables symbolTables, SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws SemanticException {

        String nodeName = ParserTreeConstants.jjtNodeName[expressionNode.getId()];
        switch (nodeName) {
            case NodeName.SUB:
            case NodeName.ADD:
            case NodeName.MUL:
            case NodeName.DIV:
            case NodeName.LESS: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isInteger(symbolTables, firstChild, functionDescriptor))
                    throw new SemanticException(firstChild);

                if(!isInteger(symbolTables, secondChild, functionDescriptor))
                    throw new SemanticException(secondChild);

                return nodeName.equals(NodeName.LESS) ? "boolean" : "int";
            }
            case NodeName.AND: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isBoolean(symbolTables, firstChild, functionDescriptor)) {
                    throw new SemanticException(firstChild);
                }

                if(!isBoolean(symbolTables, secondChild, functionDescriptor)) {
                    throw new SemanticException(secondChild);
                }

                return "boolean";
            }
            case NodeName.NOT: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                if (!isBoolean(symbolTables, firstChild, functionDescriptor)) {
                    throw new SemanticException(firstChild);
                }
                return "boolean";
            }
        }

        throw new SemanticException(expressionNode);
    }

    private static boolean isInteger(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        if (isExpression(simpleNode)) {
            String returnType = analyseExpression(symbolTables, simpleNode, functionDescriptor);
            return returnType.equals("int");
        }

        switch (nodeName) {
            case NodeName.INT:
                return true;
            case NodeName.IDENTIFIER:
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (typeDescriptor == null)
                    return false;
                else return typeDescriptor.getTypeIdentifier().equals("int");
            case NodeName.ARRAYACCESS:
                return analyseArray(false, symbolTables, simpleNode, functionDescriptor);
            case NodeName.DOTMETHOD:
                String returnType = analyseDotMethod(symbolTables, simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals("int");
        }

        return false;
    }

    private static boolean isBoolean(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        if (isExpression(simpleNode)) {
            String returnType = analyseExpression(symbolTables, simpleNode, functionDescriptor);
            return returnType.equals("boolean");
        }

        switch (nodeName) {
            case NodeName.BOOLEAN:
                return true;
            case NodeName.IDENTIFIER:
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (typeDescriptor == null)
                    return false;
                else return typeDescriptor.getTypeIdentifier().equals("boolean");
            case NodeName.DOTMETHOD:
                String returnType = analyseDotMethod(symbolTables, simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals("boolean");
        }

        return false;
    }
}
