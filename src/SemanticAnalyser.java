
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
                if (!analyseArray(true, symbolTables, simpleNode, functionDescriptor)){
                    throw new SemanticException(simpleNode);
                }
                break;
            case NodeName.DOTMETHOD: {
                String returnType = analyseDotMethod(symbolTables, simpleNode, functionDescriptor);
//                if ((returnType == null) || !returnType.equals("int"))
//                    throw new SemanticException(simpleNode);
            }

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

        SimpleNode grandchild = (SimpleNode) (isArraySize ? simpleNode.jjtGetChildren()[0] : simpleNode.jjtGetChildren()[1]);

        String nodeName = ParserTreeConstants.jjtNodeName[grandchild.getId()];
        System.out.print("NodeName: " + nodeName + "\n");
        System.out.print("NodeValue: " + grandchild.jjtGetVal() + "\n");

        switch (nodeName) {
            case NodeName.INT:
                return true;
            case NodeName.IDENTIFIER:
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(grandchild.jjtGetVal());
                if (typeDescriptor == null)
                    return false;
                else return typeDescriptor.getTypeIdentifier().equals("int");
            case NodeName.ARRAYACCESS:
                return analyseArray(false, symbolTables, grandchild, functionDescriptor);
            case NodeName.ARRAYSIZE:
                return analyseArray(true, symbolTables, grandchild, functionDescriptor);
        }

        return false;
    }

    private static String analyseDotMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        if (ParserTreeConstants.jjtNodeName[firstChild.getId()].equals(NodeName.THIS)) {
            if (ParserTreeConstants.jjtNodeName[secondChild.getId()].equals(NodeName.METHODCALL))
                return getMethodReturnType(symbolTables, secondChild, functionDescriptor);
        } else {
            switch (ParserTreeConstants.jjtNodeName[secondChild.getId()]) {
                case NodeName.LENGTH:
                    return "int";
                case NodeName.DOTMETHOD:
                    return analyseDotMethod(symbolTables, secondChild, functionDescriptor);
                default:
                    return null;
            }
        }

        return null;
    }

    private static String getMethodReturnType(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];

        StringBuilder methodIdentifier = new StringBuilder();

        if (ParserTreeConstants.jjtNodeName[firstChild.getId()].equals(NodeName.METHODNAME))
            methodIdentifier.append(firstChild.jjtGetVal());
        else
            throw new SemanticException(simpleNode);

        if (simpleNode.jjtGetChildren().length > 1) {

            SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];
            if (ParserTreeConstants.jjtNodeName[secondChild.getId()].equals(NodeName.ARGS)) {
                Node[] grandchildren = secondChild.jjtGetChildren();
                for (Node grandchildNode : grandchildren) {
                    SimpleNode grandChild = (SimpleNode) grandchildNode;
                    String nodeName = ParserTreeConstants.jjtNodeName[grandChild.getId()];
                    if (nodeName.equals(NodeName.IDENTIFIER)) {
                        String argIdentifier = grandChild.jjtGetVal();
                        methodIdentifier.append(functionDescriptor.getTypeDescriptor(argIdentifier).getTypeIdentifier());
                    } else if (nodeName.equals(NodeName.METHODCALL))
                        methodIdentifier.append(getMethodReturnType(symbolTables, grandChild, functionDescriptor));
                }
            } else
                throw new SemanticException(simpleNode);
        }

        FunctionDescriptor functionDescriptor1 = symbolTables.getFunctionDescriptor(methodIdentifier.toString());
        return (functionDescriptor1 == null) ? null : functionDescriptor1.getReturnType();
    }
}
