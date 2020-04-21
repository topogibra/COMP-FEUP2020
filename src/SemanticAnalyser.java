
public class SemanticAnalyser {
    private static final int MAX_NUM_ERRORS = 10;
    private int num_errors = 0;

    public static void analyse(SymbolTables symbolTables, SimpleNode simpleNode) throws Exception {

        if (ParserTreeConstants.jjtNodeName[simpleNode.getId()].equals(NodeName.METHOD)) {
            String methodIdentifier = getMethodIdentifier(simpleNode);
            System.out.print("Method id: " + methodIdentifier + "\n");
            analyseMethod(symbolTables, simpleNode, symbolTables.getFunctionDescriptor(methodIdentifier));
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

    public static boolean analyseArray(boolean isArraySize, SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
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

        switch (nodeName) {
            case NodeName.INT:
                return true;
            case NodeName.IDENTIFIER:
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(analysedChild.jjtGetVal());
                if (typeDescriptor == null)
                    return false;
                else return typeDescriptor.getTypeIdentifier().equals("int");
            case NodeName.ARRAYACCESS:
                return analyseArray(false, symbolTables, analysedChild, functionDescriptor);
            case NodeName.ARRAYSIZE:
                return analyseArray(true, symbolTables, analysedChild, functionDescriptor);
            case NodeName.DOTMETHOD:
                String returnType = analyseDotMethod(symbolTables, analysedChild, functionDescriptor);
                return (returnType != null) && returnType.equals("int");
        }

        return false;
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

        if (nodeName.equals(NodeName.THIS))
            return true;
        else if (nodeName.equals(NodeName.IDENTIFIER)) {
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
            return typeDescriptor != null && typeDescriptor.getTypeIdentifier().equals(symbolTables.getClassName());
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
                    switch (nodeName) {
                        case NodeName.IDENTIFIER:
                            String argIdentifier = grandChild.jjtGetVal();

                            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(argIdentifier);
                            if (typeDescriptor != null)
                                methodIdentifier.append(typeDescriptor.getTypeIdentifier());
                            else
                                throw new SemanticException(grandChild);
                            break;
                        case NodeName.DOTMETHOD:
                            String returnType = analyseDotMethod(symbolTables, grandChild, functionDescriptor);
                            if (returnType != null)
                                methodIdentifier.append(returnType);
                            else
                                throw new SemanticException(grandChild);
                            break;
                        case NodeName.ARRAYACCESS:
                            //TODO get return value of array access
                            break;
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
                return true;
            default:
                return false;
        }
    }

    private static String analyseExpression(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws SemanticException {
        //TODO

        return "";
    }
}
