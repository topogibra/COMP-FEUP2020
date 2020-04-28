public class SemanticAnalyser {
    private final int MAX_NUM_ERRORS = 10;
    private int no_error = 0;

    private final SymbolTables symbolTables;
    private final SimpleNode root;
    private final boolean ignore_exceptions;

    public SemanticAnalyser(SymbolTables symbolTables, SimpleNode root, boolean ignore_exceptions) {
        this.symbolTables = symbolTables;
        this.root = root;
        this.ignore_exceptions = ignore_exceptions;
    }

    private void addException(SemanticException exception) throws Exception {
        if (!ignore_exceptions) {
            System.err.println(exception.getMessage());
            no_error++;
            if (no_error >= MAX_NUM_ERRORS) {
                throw new Exception("Reached max number of semantic errors");
            }
        }
    }

    public void startAnalyse() throws Exception {
        this.analyse();

        if (this.no_error > 0) {
            int errors = this.no_error;
            this.no_error = 0;
            throw new Exception("Found " + errors + " semantic errors");
        }

        this.no_error = 0;
    }

    private void analyse() throws Exception {
        Node[] children = this.root.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child.getNodeName().equals(NodeName.CLASS)) {
                this.analyseClass(child);
            }
        }
    }

    private void analyseClass(SimpleNode classNode) throws Exception {
        Node[] children = classNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            switch (child.getNodeName()) {
                case NodeName.METHOD: {
                    String methodIdentifier = Utils.getMethodIdentifier(child);
                    this.analyseMethod(child, symbolTables.getFunctionDescriptor(methodIdentifier));
                    break;
                }
                case NodeName.EXTENDS: {
                    String extendedClassName = child.getChild(0).jjtGetVal();
                    if (!symbolTables.isImportedClass(extendedClassName)) {
                        this.addException(new SemanticException(child)); // TODO Make the exception more specific
                        return;
                    }
                    break;
                }
                case NodeName.VARDECLARATION: {
                    this.analyseVarDeclaration(child);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void analyseMethod(SimpleNode methodNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = methodNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child.getNodeName().equals(NodeName.METHODBODY))
                this.analyseMethodBody(child, functionDescriptor);
        }
    }

    private void analyseVarDeclaration(SimpleNode varDeclarationNode) throws Exception {
        SimpleNode typeNode = (SimpleNode) varDeclarationNode.jjtGetChild(0);

        String type = typeNode.jjtGetVal();
        switch (type) {
            case VarTypes.INT:
            case VarTypes.INTARRAY:
            case VarTypes.BOOLEAN:
                break;
            default: {
                if (!symbolTables.getClassName().equals(type)) { //TODO check if we can declare a variable of an imported type
                    this.addException(new NotValidType(varDeclarationNode));
                }
                break;
            }
        }

    }

    private void analyseMethodBody(SimpleNode methodBodyNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = methodBodyNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;
            String childName = ParserTreeConstants.jjtNodeName[child.getId()];

            switch (childName) {
                case NodeName.ARRAYACCESS:
                    this.analyseArray(false, child, functionDescriptor);
                    break;
                case NodeName.ARRAYSIZE:
                    this.analyseArray(true, child, functionDescriptor);
                    break;
                case NodeName.DOTMETHOD:
                    this.analyseDotMethod(child, functionDescriptor);
                    break;
                case NodeName.ASSIGNMENT:
                    this.analyseAssignment(child, functionDescriptor);
                    break;
                case NodeName.VARDECLARATION: {
                    this.analyseVarDeclaration(child);
                    break;
                }
                default:
                    break;
            }
        }
    }


    private boolean analyseArray(boolean isArraySize, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];

        if (!isArraySize) { //Check if it is an array that's being accessed
            TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(firstChild.jjtGetVal());
            if (typeDescriptor == null) {
                this.addException(new NotDeclared(firstChild));
                return false;
            }
            if (!typeDescriptor.isArray()) {
                this.addException(new ExpectedArray(firstChild, typeDescriptor.getTypeIdentifier()));
                return false;
            }
            if (!typeDescriptor.isInit()) {
                this.addException(new VarNotInitialized(simpleNode));
                return false;
            }
        }

        SimpleNode analysedChild = (SimpleNode) (isArraySize ? firstChild : simpleNode.jjtGetChildren()[1]);

        // Analyses if value inside array brackets is a positive integer
        if (!isInteger(analysedChild, functionDescriptor)) {
            this.addException(new IndexNotInt(analysedChild));
            return false;
        }

        return true;
    }

    private String analyseDotMethod(SimpleNode dotMethodNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) dotMethodNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) dotMethodNode.jjtGetChildren()[1];

        String secondChildName = ParserTreeConstants.jjtNodeName[secondChild.getId()];

        if (isInteger(firstChild, functionDescriptor, true) || isBoolean(firstChild, functionDescriptor, true)) {
            addException(new SemanticException(firstChild));
            return null;
        }

        if (Utils.isClassVariable(symbolTables, firstChild, functionDescriptor)) { // [ClassName | new ClassName].method
            if (secondChildName.equals(NodeName.METHODCALL))
                return getMethodReturnType(secondChild, functionDescriptor);
        } else { // Call a method from an import
            ImportDescriptor importDescriptor = Utils.getImportedMethod(symbolTables, dotMethodNode, functionDescriptor);
            if (importDescriptor != null) {
                return importDescriptor.getReturnType().getTypeIdentifier();
            } else if (secondChildName.equals(NodeName.LENGTH))
                return VarTypes.INT;
        }

        addException(new SemanticException(dotMethodNode));
        return null;
    }

    private String getMethodReturnType(SimpleNode methodCallNode, FunctionDescriptor functionDescriptor) throws Exception {
        String methodIdentifier = parseMethodIdentifier(methodCallNode, functionDescriptor);
        FunctionDescriptor methodDescriptor = symbolTables.getFunctionDescriptor(methodIdentifier);
        return (methodDescriptor == null) ? null : methodDescriptor.getReturnType();
    }

    public String parseMethodIdentifier(SimpleNode methodCallNode, FunctionDescriptor functionDescriptor) throws Exception {
        StringBuilder methodIdentifier = new StringBuilder();

        if (methodCallNode.jjtGetChildren() == null || methodCallNode.jjtGetChildren().length == 0)
            return null;

        SimpleNode firstChild = (SimpleNode) methodCallNode.jjtGetChildren()[0];
        if (firstChild.getNodeName().equals(NodeName.METHODNAME))
            methodIdentifier.append(firstChild.jjtGetVal());
        else {
            addException(new SemanticException(methodCallNode)); // TODO Make the exception more specific
            return null;
        }

        if (methodCallNode.jjtGetChildren().length > 1) { //Check if method call has arguments

            SimpleNode secondChild = (SimpleNode) methodCallNode.jjtGetChildren()[1];
            if (ParserTreeConstants.jjtNodeName[secondChild.getId()].equals(NodeName.ARGS)) {

                for (Node grandchildNode : secondChild.jjtGetChildren()) {
                    SimpleNode grandChild = (SimpleNode) grandchildNode;

                    String nodeName = ParserTreeConstants.jjtNodeName[grandChild.getId()];
                    if (Utils.isExpression(grandChild)) {
                        String returnType = analyseExpression(grandChild, functionDescriptor);
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
                            String returnType = analyseDotMethod(grandChild, functionDescriptor);
                            if (returnType != null)
                                methodIdentifier.append(returnType);
                            else
                                addException(new SemanticException(grandChild)); // TODO Make the exception more specific
                            break;
                        }
                        case NodeName.ARRAYACCESS: {
                            if (analyseArray(false, grandChild, functionDescriptor)) {
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
                addException(new SemanticException(methodCallNode)); // TODO Make the exception more specific
        }

        return methodIdentifier.toString();
    }

    private String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {

        String nodeName = ParserTreeConstants.jjtNodeName[expressionNode.getId()];
        switch (nodeName) {
            case NodeName.SUB:
            case NodeName.ADD:
            case NodeName.MUL:
            case NodeName.DIV:
            case NodeName.LESS: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isInteger(firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return null;
                }

                if (!isInteger(secondChild, functionDescriptor)) {
                    addException(new SemanticException(secondChild)); // TODO Make the exception more specific
                    return null;
                }

                return nodeName.equals(NodeName.LESS) ? VarTypes.BOOLEAN : VarTypes.INT;
            }
            case NodeName.AND: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isBoolean(firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return null;
                }

                if (!isBoolean(secondChild, functionDescriptor)) {
                    addException(new SemanticException(secondChild)); // TODO Make the exception more specific
                    return null;
                }

                return VarTypes.BOOLEAN;
            }
            case NodeName.NOT: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];

                if (!isBoolean(firstChild, functionDescriptor)) {
                    addException(new SemanticException(firstChild)); // TODO Make the exception more specific
                    return "";
                }

                return VarTypes.BOOLEAN;
            }
        }

        addException(new SemanticException(expressionNode)); // TODO Make the exception more specific
        return null;
    }

    private boolean isInteger(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String nodeName = ParserTreeConstants.jjtNodeName[simpleNode.getId()];

        if (Utils.isExpression(simpleNode)) {
            String returnType = analyseExpression(simpleNode, functionDescriptor);
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
                return analyseArray(false, simpleNode, functionDescriptor);
            case NodeName.DOTMETHOD: {
                String returnType = analyseDotMethod(simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals(VarTypes.INT);
            }
        }

        return false;
    }

    private boolean isBoolean(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String nodeName = simpleNode.getNodeName();

        if (Utils.isExpression(simpleNode)) {
            String returnType = analyseExpression(simpleNode, functionDescriptor);
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
                String returnType = analyseDotMethod(simpleNode, functionDescriptor);
                return (returnType != null) && returnType.equals(VarTypes.BOOLEAN);
        }

        return false;
    }

    private boolean isBoolean(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isBoolean(simpleNode, functionDescriptor, false);
    }

    private boolean isInteger(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isInteger(simpleNode, functionDescriptor, false);
    }


    private void analyseAssignment(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode leftSide = (SimpleNode) simpleNode.jjtGetChildren()[0];
        SimpleNode rightSide = (SimpleNode) simpleNode.jjtGetChildren()[1];

        String leftSideName = ParserTreeConstants.jjtNodeName[leftSide.getId()];
        String leftType;
        switch (leftSideName) {
            case NodeName.ARRAYACCESS: {
                if (!analyseArray(false, leftSide, functionDescriptor)) {
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

        if (Utils.isExpression(rightSide)) {
            String rightType = analyseExpression(rightSide, functionDescriptor);
            if (!leftType.equals(rightType)) {
                addException(new NotSameType(simpleNode, leftType, rightType));
                return;
            }

            functionDescriptor.getScope().setInit(leftSide.jjtGetVal(), true);
            return;
        }

        String rightSideName = rightSide.getNodeName();
        switch (rightSideName) {
            case NodeName.IDENTIFIER: { // a = anothervar;
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
                String returnType = analyseDotMethod(rightSide, functionDescriptor);
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
                if (!analyseArray(false, rightSide, functionDescriptor)) {
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
                        if (!analyseArray(true, childNode, functionDescriptor))
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
