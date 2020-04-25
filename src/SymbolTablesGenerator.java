public class SymbolTablesGenerator {

    public static SymbolTables generate(SimpleNode root) {
        SymbolTables symbolTables = new SymbolTables();

        Node[] children = root.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                   case NodeName.IMPORT:
                        symbolTables.addImport(createImportDescriptor(child));
                        break;
                   case NodeName.CLASS:
                        parseClass(child, symbolTables);
                        break;
                }
            }
        }

        symbolTables.print();
        return symbolTables;
    }

    public static ImportDescriptor createImportDescriptor(SimpleNode simpleNode) {
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
                    case NodeName.ARGTYPE: {
                        String type = child.jjtGetVal();
                        if (!type.equals(VarTypes.VOID))
                            importDescriptor.addArgument(new TypeDescriptor(child.jjtGetVal()));
                        break;
                    }
                    case NodeName.RETURNTYPE:
                        importDescriptor.setReturnType(new TypeDescriptor(child.jjtGetVal()));
                }
            }
        }

        return importDescriptor;
    }

    public static void parseClass(SimpleNode simpleNode, SymbolTables symbolTables) {

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
                        String type = ((SimpleNode) grandchildren[0]).jjtGetVal();
                        TypeDescriptor typeDescriptor;
                        if (type.equals(VarTypes.INTARRAY))
                            typeDescriptor = new ArrayDescriptor(type);
                        else
                            typeDescriptor = new TypeDescriptor(type);
                        symbolTables.addVar(((SimpleNode) grandchildren[1]).jjtGetVal(), typeDescriptor);
                        break;
                    case NodeName.METHOD:
                        symbolTables.addMethod(createFunctionDescriptor(child, symbolTables.getScope()));
                        break;
                }
            }
        }
    }

    public static FunctionDescriptor createFunctionDescriptor(SimpleNode simpleNode, Scope parentScope) {
        FunctionDescriptor functionDescriptor = new FunctionDescriptor(parentScope);

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                switch (ParserTreeConstants.jjtNodeName[child.getId()]) {
                    case NodeName.RETURNTYPE:
                        functionDescriptor.setReturnType(child.jjtGetVal());
                        break;
                    case NodeName.IDENTIFIER:
                        functionDescriptor.setMethodName(child.jjtGetVal());
                        break;
                    case NodeName.ARGS:
                        parseFunctionArguments(functionDescriptor, child);
                        break;
                    case NodeName.METHODBODY:
                        parseMethodBody(functionDescriptor, child);
                        break;
                }
            }
        }

        return functionDescriptor;
    }

    public static void parseFunctionArguments(FunctionDescriptor functionDescriptor, SimpleNode simpleNode) {

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                Node[] grandchildren = child.jjtGetChildren();
                String type = ((SimpleNode) grandchildren[0]).jjtGetVal();
                TypeDescriptor typeDescriptor;
                if (type.equals(VarTypes.INTARRAY))
                    typeDescriptor = new ArrayDescriptor(type);
                else
                    typeDescriptor = new TypeDescriptor(type);

                functionDescriptor.addParam(((SimpleNode) grandchildren[1]).jjtGetVal(), typeDescriptor);
            }
        }
    }

    public static void parseMethodBody(FunctionDescriptor functionDescriptor, SimpleNode simpleNode) {

        Node[] children = simpleNode.jjtGetChildren();
        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child != null) {
                if (ParserTreeConstants.jjtNodeName[child.getId()].equals(NodeName.VARDECLARATION)) {
                    Node[] grandchildren = child.jjtGetChildren();
                    String type = ((SimpleNode) grandchildren[0]).jjtGetVal();
                    TypeDescriptor typeDescriptor;
                    if (type.equals(VarTypes.INTARRAY))
                        typeDescriptor = new ArrayDescriptor(type);
                    else
                        typeDescriptor = new TypeDescriptor(type);
                    functionDescriptor.addVar(((SimpleNode) grandchildren[1]).jjtGetVal(), typeDescriptor);
                }
            }
        }
    }
}
