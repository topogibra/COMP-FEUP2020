import java.util.ArrayList;

public class SemanticAnalyser {
    private static final int MAX_NUM_ERRORS = 10;
    private static int no_error = 0;
    private static boolean ignore_exceptions = false;

    public static SymbolTables startAnalyse(SimpleNode root) throws Exception {
        SymbolTables symbolTables = SymbolTablesGenerator.generate(root);
        ignore_exceptions = false;
        analyse(symbolTables, root);

        if (no_error > 0) {
            int errors = no_error;
            no_error = 0;
            throw new Exception("Found " + errors + " semantic errors");
        }
        no_error = 0;

        ignore_exceptions = false;
        return symbolTables;
    }

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
                if (!symbolTables.isImportedClass(extendedClassName)) {
                    addException(new SemanticException(simpleNode)); // TODO Make the exception more specific
                    return;
                }
                break;
            }
            case NodeName.VARDECLARATION: {
                analyseVarDeclaration(symbolTables, simpleNode);
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

    public static void addException(SemanticException exception) throws Exception {
        if (!ignore_exceptions) {
            System.err.println(exception.getMessage());
            no_error++;
            if (no_error >= MAX_NUM_ERRORS) {
                throw new Exception("Reached max number of semantic errors");
            }
        }
    }

    public static void analyseMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = simpleNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;
            String childName = ParserTreeConstants.jjtNodeName[child.getId()];

            if (childName.equals(NodeName.VARDECLARATION)) {
                analyseVarDeclaration(symbolTables, simpleNode);
            } else if (childName.equals(NodeName.METHODBODY))
                analyseMethodBody(symbolTables, child, functionDescriptor);
        }
    }

    private static void analyseVarDeclaration(SymbolTables symbolTables, SimpleNode simpleNode) throws Exception {
        SimpleNode typeNode = (SimpleNode) simpleNode.jjtGetChild(0);

        String type = typeNode.jjtGetVal();
        switch (type) {
            case VarTypes.INT:
            case VarTypes.INTARRAY:
            case VarTypes.BOOLEAN:
                break;
            default:
                if (!symbolTables.getClassName().equals(type)) {
                    addException(new NotValidType(simpleNode));
                }
                break;
        }

    }

    public static void analyseMethodBody(SymbolTables symbolTables, SimpleNode methodBodyNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = methodBodyNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;
            String childName = ParserTreeConstants.jjtNodeName[child.getId()];

            switch (childName) {
                case NodeName.ARRAYACCESS:
                    if (!analyseArray(false, symbolTables, child, functionDescriptor)) {
                        continue;
                    }
                    break;
                case NodeName.ARRAYSIZE:
                    if (!analyseArray(true, symbolTables, child, functionDescriptor)) {
                        continue;
                    }
                    break;
                case NodeName.DOTMETHOD:
                    if (analyseDotMethod(symbolTables, child, functionDescriptor) == null) {
                        addException(new SemanticException(child)); // TODO Make the exception more specific
                        continue;
                    }
                    break;
                case NodeName.ASSIGNMENT: {
                    analyseAssignment(symbolTables, child, functionDescriptor);
                    break;
                }
                default:
                    break;
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
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];

        if (!isArraySize) { //Check if it is an array that's being accessed
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(firstChild.jjtGetVal());
            if (typeDescriptor == null) {
                addException(new NotDeclared(firstChild));
                return false;
            }
            if (!typeDescriptor.isArray()) {
                addException(new ExpectedArray(firstChild, typeDescriptor.getTypeIdentifier()));
                return false;
            }
            if (!typeDescriptor.isInit()) {
                addException(new VarNotInitialized(simpleNode));
                return false;
            }
        }

        SimpleNode analysedChild = (SimpleNode) (isArraySize ? firstChild : simpleNode.jjtGetChildren()[1]);

        // Analyses if value inside array access is integer
        if (!isInteger(symbolTables, analysedChild, functionDescriptor)) {
            addException(new IndexNotInt(analysedChild));
            return false;
        }

        return true;
    }

    private static String analyseDotMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String secondChildName = ParserTreeConstants.jjtNodeName[secondChild.getId()];

        if (isInteger(symbolTables, firstChild, functionDescriptor, true) || isBoolean(symbolTables, firstChild, functionDescriptor, true)) {
            addException(new SemanticException(firstChild));
            return "";
        }

        if (isClassVariable(symbolTables, firstChild, functionDescriptor)) { // [ClassName | new ClassName].method
            if (secondChildName.equals(NodeName.METHODCALL))
                return getMethodReturnType(symbolTables, secondChild, functionDescriptor);
        } else { // Call a method from an import
            ImportDescriptor importDescriptor = getImportedMethod(symbolTables, simpleNode, functionDescriptor);
            if (importDescriptor != null) {
                System.out.println("Import class: " + importDescriptor.getClassName() + "; Method: " + importDescriptor.getMethodName());
                return importDescriptor.getReturnType().getTypeIdentifier();
            } else if (secondChildName.equals(NodeName.LENGTH))
                return VarTypes.INT;
        }

        return null;
    }

    public static boolean isClassVariable(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) {
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
        }

        return false;
    }

    private static String getMethodReturnType(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        String methodIdentifier = parseMethodIdentifier(symbolTables, simpleNode, functionDescriptor);
        FunctionDescriptor methodDescriptor = symbolTables.getFunctionDescriptor(methodIdentifier);
        return (methodDescriptor == null) ? null : methodDescriptor.getReturnType();
    }

    public static String parseMethodIdentifier(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder methodIdentifier = new StringBuilder();

        if (simpleNode.jjtGetChildren() == null || simpleNode.jjtGetChildren().length == 0)
            return "";

        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        if (ParserTreeConstants.jjtNodeName[firstChild.getId()].equals(NodeName.METHODNAME))
            methodIdentifier.append(firstChild.jjtGetVal());
        else {
            addException(new SemanticException(simpleNode)); // TODO Make the exception more specific
            return "";
        }

        if (simpleNode.jjtGetChildren().length > 1) { //Check if method call has arguments

            SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];
            if (ParserTreeConstants.jjtNodeName[secondChild.getId()].equals(NodeName.ARGS)) {

                for (Node grandchildNode : secondChild.jjtGetChildren()) {
                    SimpleNode grandChild = (SimpleNode) grandchildNode;

                    String nodeName = ParserTreeConstants.jjtNodeName[grandChild.getId()];
                    if (isExpression(grandChild)) {
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
                                addException(new SemanticException(grandChild)); // TODO Make the exception more specific
                            break;
                        }
                        case NodeName.DOTMETHOD: {
                            String returnType = analyseDotMethod(symbolTables, grandChild, functionDescriptor);
                            if (returnType != null)
                                methodIdentifier.append(returnType);
                            else
                                addException(new SemanticException(grandChild)); // TODO Make the exception more specific
                            break;
                        }
                        case NodeName.ARRAYACCESS: {
                            if (analyseArray(false, symbolTables, grandChild, functionDescriptor)) {
                                methodIdentifier.append(VarTypes.INT);
                            }
                            break;
                        }
                        case NodeName.INT: {
                            methodIdentifier.append(VarTypes.INT);
                            break;
                        }
                        case NodeName.BOOLEAN: {
                            methodIdentifier.append(VarTypes.BOOLEAN);
                            break;
                        }
                        default:
                            addException(new SemanticException(grandChild)); // TODO Make the exception more specific
                    }
                }
            } else
                addException(new SemanticException(simpleNode)); // TODO Make the exception more specific
        }

        return methodIdentifier.toString();
    }

    public static ImportDescriptor getImportedMethod(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String importedMethodIdentifier = firstChild.jjtGetVal();
        importedMethodIdentifier += parseMethodIdentifier(symbolTables, secondChild, functionDescriptor);

        return symbolTables.getImportDescriptor(importedMethodIdentifier);
    }

    public static boolean isExpression(SimpleNode simpleNode) {
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

    private static String analyseExpression(SymbolTables symbolTables, SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {

        String nodeName = ParserTreeConstants.jjtNodeName[expressionNode.getId()];
        switch (nodeName) {
            case NodeName.SUB:
            case NodeName.ADD:
            case NodeName.MUL:
            case NodeName.DIV:
            case NodeName.LESS: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isInteger(symbolTables, firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return "";
                }

                if (!isInteger(symbolTables, secondChild, functionDescriptor)) {
                    addException(new SemanticException(secondChild)); // TODO Make the exception more specific
                    return "";
                }

                return nodeName.equals(NodeName.LESS) ? VarTypes.BOOLEAN : VarTypes.INT;
            }
            case NodeName.AND: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isBoolean(symbolTables, firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return "";
                }

                if (!isBoolean(symbolTables, secondChild, functionDescriptor)) {
                    addException(new SemanticException(secondChild)); // TODO Make the exception more specific
                    return "";
                }

                return VarTypes.BOOLEAN;
            }
            case NodeName.NOT: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                if (!isBoolean(symbolTables, firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return "";
                }
                return VarTypes.BOOLEAN;
            }
        }

        addException(new SemanticException(expressionNode)); // TODO Make the exception more specific
        return "";
    }

    private static boolean isInteger(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        if (isExpression(simpleNode)) {
            String returnType = analyseExpression(symbolTables, simpleNode, functionDescriptor);
            return returnType.equals(VarTypes.INT);
        }

        switch (nodeName) {
            case NodeName.INT:
                return true;
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (!ignore_init) {
                    if (typeDescriptor == null) {
                        addException(new NotDeclared(simpleNode));
                        return false;
                    }
                    if (!typeDescriptor.isInit()) {
                        addException(new VarNotInitialized(simpleNode));
                        return false;
                    }
                    return typeDescriptor.getTypeIdentifier().equals(VarTypes.INT);
                }
                break;
            }

            case NodeName.ARRAYACCESS:
                return analyseArray(false, symbolTables, simpleNode, functionDescriptor);
            case NodeName.DOTMETHOD: {
                String returnType = analyseDotMethod(symbolTables, simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals(VarTypes.INT);

            }
        }

        return false;
    }

    private static boolean isBoolean(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        if (isExpression(simpleNode)) {
            String returnType = analyseExpression(symbolTables, simpleNode, functionDescriptor);
            return returnType.equals(VarTypes.BOOLEAN);
        }

        switch (nodeName) {
            case NodeName.BOOLEAN:
                return true;
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(simpleNode.jjtGetVal());
                if (!ignore_init) {
                    if (typeDescriptor == null)
                        return false;
                    if (!typeDescriptor.isInit()) {
                        addException(new VarNotInitialized(simpleNode));
                        return false;
                    }
                    return typeDescriptor.getTypeIdentifier().equals(VarTypes.BOOLEAN);
                }
                break;
            }
            case NodeName.DOTMETHOD:
                String returnType = analyseDotMethod(symbolTables, simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals(VarTypes.BOOLEAN);
        }

        return false;
    }

    private static boolean isBoolean(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isBoolean(symbolTables, simpleNode, functionDescriptor, false);
    }

    private static boolean isInteger(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isInteger(symbolTables, simpleNode, functionDescriptor, false);
    }


    private static void analyseAssignment(SymbolTables symbolTables, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode leftSide = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode rightSide = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String leftSideName = ParserTreeConstants.jjtNodeName[leftSide.getId()];
        String leftType;
        switch (leftSideName) {
            case NodeName.ARRAYACCESS: {
                if (!analyseArray(false, symbolTables, leftSide, functionDescriptor)) {
                    return;
                }
                leftType = VarTypes.INT;
                break;
            }
            case NodeName.IDENTIFIER: {
                TypeDescriptor tmp = functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
                if (tmp == null) {
                    addException(new NotDeclared(leftSide));
                    return;
                } else
                    leftType = tmp.getTypeIdentifier();
                break;
            }
            default: {
                addException(new SemanticException(simpleNode)); // TODO Make the exception more specific
                return;
            }
        }

        if (isExpression(rightSide)) {
            String rightType = analyseExpression(symbolTables, rightSide, functionDescriptor);
            if (!leftType.equals(rightType)) {
                addException(new NotSameType(simpleNode, leftType, rightType));
                return;
            }

            functionDescriptor.getScope().setInit(leftSide.jjtGetVal(), true);
            return;
        }

        String rightSideName = ParserTreeConstants.jjtNodeName[rightSide.getId()];
        switch (rightSideName) {
            case NodeName.IDENTIFIER: { // a = anothervar;
                String rightSideValue = rightSide.jjtGetVal();
                System.out.println("Right side: " + rightSideValue);
                TypeDescriptor tmp = functionDescriptor.getTypeDescriptor(rightSide.jjtGetVal());
                if (tmp == null) { //Not declared
                    addException(new NotDeclared(rightSide));
                    return;
                }

                String rightType = tmp.getTypeIdentifier();
                if (!rightType.equals(leftType)) {
                    addException(new NotSameType(rightSide, leftType, rightType));
                    return;
                } else if (!tmp.isInit()) {
                    addException(new VarNotInitialized(rightSide));
                    return;
                }
                break;
            }
            case NodeName.DOTMETHOD: {
                String returnType = analyseDotMethod(symbolTables, rightSide, functionDescriptor);
                if (returnType == null) {
                    addException(new NotDeclared(rightSide));
                    return;
                } else if (!returnType.equals(leftType)) {
                    addException(new NotSameType(simpleNode, leftType, returnType));
                    return;
                }
                break;
            }
            case NodeName.ARRAYACCESS: {
                if (!analyseArray(false, symbolTables, rightSide, functionDescriptor)) {
                    return;
                }
                break;
            }
            case NodeName.BOOLEAN: {
                if (!leftType.equals(VarTypes.BOOLEAN)) {
                    addException(new NotSameType(rightSide, leftType, VarTypes.BOOLEAN));
                    return;
                }
                break;
            }
            case NodeName.INT: {
                if (!leftType.equals(VarTypes.INT)) {
                    addException(new NotSameType(rightSide, leftType, VarTypes.INT));
                    return;
                }
                break;
            }
            case NodeName.NEW: {
                SimpleNode childNode = (SimpleNode) rightSide.jjtGetChild(0);
                String childNodeName = ParserTreeConstants.jjtNodeName[childNode.getId()];
                switch (childNodeName) {
                    case NodeName.ARRAYSIZE: { // new int[1]
                        if (!analyseArray(true, symbolTables, childNode, functionDescriptor))
                            return;
                        break;
                    }
                    case NodeName.IDENTIFIER: { // new ClassName();
                        if (!symbolTables.getClassName().equals(childNode.jjtGetVal())) {
                            addException(new SemanticException(childNode)); // TODO Make the exception more specific
                            return;
                        }
                        else if (!leftType.equals(symbolTables.getClassName())) {
                            addException(new SemanticException(childNode));
                            return;
                        }
                        break;
                    }
                }
                break;
            }
            default: {
                addException(new SemanticException(simpleNode)); // TODO Make the exception more specific
                return;
            }

        }

        functionDescriptor.getScope().setInit(leftSide.jjtGetVal(), true);
    }
}
