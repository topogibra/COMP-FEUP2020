
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
                if (!analyseArrayAccess(symbolTables, simpleNode, functionDescriptor))
                    throw new Exception("ola");

        }
    }

    public static String getMethodIdentifier(SimpleNode simpleNode) {
        StringBuilder stringBuilder = new StringBuilder();

        Node[] children = simpleNode.jjtGetChildren();

        stringBuilder.append( ((SimpleNode) children[1]).jjtGetVal()); // Method Name

        if (!ParserTreeConstants.jjtNodeName[((SimpleNode) children[2]).getId()].equals(NodeName.ARGS))
            return stringBuilder.toString();

        Node[] grandchildren = ((SimpleNode) children[2]).jjtGetChildren();
        for (Node node : grandchildren) {
            SimpleNode simpleNode1 = (SimpleNode) node;
            stringBuilder.append(((SimpleNode) simpleNode1.jjtGetChildren()[0]).jjtGetVal());
        }

        return stringBuilder.toString();
    }

    public static boolean analyseArrayAccess(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {

        SimpleNode grandchild = (SimpleNode) simpleNode.jjtGetChildren()[1];
        String nodeName = ParserTreeConstants.jjtNodeName[grandchild.getId()];
        if (nodeName.equals(NodeName.INT)) {
            return true;
        }
        else if (nodeName.equals(NodeName.IDENTIFIER) ) {
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(grandchild.jjtGetVal());
            if (typeDescriptor == null)
                return false;
            else return typeDescriptor.getTypeIdentifier().equals("int");
        }
        else if (nodeName.equals(NodeName.ARRAYACCESS)) {
            return analyseArrayAccess(symbolTables, grandchild, functionDescriptor);
        }

        return false;
    }
}
