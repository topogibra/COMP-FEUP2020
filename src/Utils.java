import Types.NodeName;

public class Utils {

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

    public static boolean isClassVariable(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        switch (nodeName) {
            case NodeName.THIS:
                return true;
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (typeDescriptor != null) {
                    String typeIdentifier = typeDescriptor.getTypeIdentifier();
                    return typeIdentifier.equals(symbolTables.getClassName());
                }
                return false;
            }
            case NodeName.NEW: {
                SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
                return firstChild.jjtGetVal().equals(symbolTables.getClassName());
            }
            case NodeName.DOTMETHOD: {
                SemanticAnalyser sa = new SemanticAnalyser(symbolTables, simpleNode, true);
                String res = sa.analyseDotMethod(simpleNode, functionDescriptor);
                return res != null && res.equals(symbolTables.getClassName());
            }
        }

        return false;
    }

    public static boolean isArithmeticExpression(SimpleNode simpleNode) {
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

    public static ImportDescriptor getImportedMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String importedMethodIdentifier = firstChild.jjtGetVal();
        importedMethodIdentifier += parseMethodIdentifier(symbolTables, secondChild, functionDescriptor);

        return symbolTables.getImportDescriptor(importedMethodIdentifier);
    }

    public static String parseMethodIdentifier(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SemanticAnalyser semanticAnalyser = new SemanticAnalyser(symbolTables, null, true);
        return semanticAnalyser.parseMethodIdentifier(simpleNode, functionDescriptor);
    }



}