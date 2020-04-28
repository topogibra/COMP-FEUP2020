import Types.NodeName;

public class SymbolTablesGenerator {
    private final SimpleNode root;

    public SymbolTablesGenerator(SimpleNode root) {
        this.root = root;
    }

    public SymbolTables generate() {
        SymbolTables symbolTables = new SymbolTables();

        Node[] children = this.root.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                   case NodeName.IMPORT:
                        symbolTables.addImport(createImportDescriptor(child));
                        break;
                   case NodeName.CLASS:
                        this.parseClass(child, symbolTables);
                        break;
                }
            }
        }

        symbolTables.print();
        return symbolTables;
    }

    private ImportDescriptor createImportDescriptor(SimpleNode simpleNode) {
        ImportDescriptor importDescriptor = new ImportDescriptor();

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                    case NodeName.STATIC:
                        importDescriptor.setStatic(true);
                        break;
                    case NodeName.CLASSNAME:
                        importDescriptor.setClassName(child.jjtGetVal());
                        break;
                    case NodeName.METHODNAME:
                        importDescriptor.setMethodName(child.jjtGetVal());
                        break;
                    case NodeName.ARGTYPE:
                        importDescriptor.addArgument(new TypeDescriptor(child.jjtGetVal()));
                        break;
                    case NodeName.RETURNTYPE:
                        importDescriptor.setReturnType(new TypeDescriptor(child.jjtGetVal()));
                }
            }
        }

        return importDescriptor;
    }

    private void parseClass(SimpleNode simpleNode, SymbolTables symbolTables) {

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                    case NodeName.IDENTIFIER:
                        symbolTables.setClassName(child.jjtGetVal());
                        break;
                    case NodeName.EXTENDS:
                        String extendedClassName = ((SimpleNode) child.jjtGetChild(0)).jjtGetVal();
                        symbolTables.setExtendedClass(extendedClassName);
                        break;
                    case NodeName.VARDECLARATION:
                        Node[] grandchildren = child.jjtGetChildren();
                        symbolTables.addVar(((SimpleNode) grandchildren[1]).jjtGetVal(), new TypeDescriptor(((SimpleNode) grandchildren[0]).jjtGetVal()));
                        break;
                    case NodeName.METHOD:
                        symbolTables.addMethod(this.createFunctionDescriptor(child, symbolTables.getScope()));
                        break;
                }
            }
        }
    }

    private FunctionDescriptor createFunctionDescriptor(SimpleNode simpleNode, Scope parentScope) {
        FunctionDescriptor functionDescriptor = new FunctionDescriptor(parentScope, simpleNode);

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                    case NodeName.RETURNTYPE:{
                        functionDescriptor.setReturnType(child.jjtGetVal());
                        break;}
                    case NodeName.IDENTIFIER:{
                        functionDescriptor.setMethodName(child.jjtGetVal());
                        break;}
                    case NodeName.ARGS:{
                        this.parseFunctionArguments(functionDescriptor, child);
                        break;}
                    case NodeName.METHODBODY:{
                        this.parseMethodBody(functionDescriptor, child);
                        break;}
                }
            }
        }

        return functionDescriptor;
    }

    private void parseFunctionArguments(FunctionDescriptor functionDescriptor, SimpleNode simpleNode) {
        int index = 1;
        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                Node[] grandchildren = child.jjtGetChildren();
                functionDescriptor.addParam( ((SimpleNode) grandchildren[1]).jjtGetVal(), new TypeDescriptor( ((SimpleNode) grandchildren[0]).jjtGetVal(), index));
            }

            index++;
        }
    }

    private void parseMethodBody(FunctionDescriptor functionDescriptor, SimpleNode simpleNode) {
        int index = functionDescriptor.getParams().size() + 1;
        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                if (ParserTreeConstants.jjtNodeName[child.getId()].equals(NodeName.VARDECLARATION)) {
                    Node[] grandchildren = child.jjtGetChildren();
                    functionDescriptor.addVar(((SimpleNode) grandchildren[1]).jjtGetVal(), new TypeDescriptor(((SimpleNode) grandchildren[0]).jjtGetVal(), index));
                }
            }
            index++;
        }
    }
}
