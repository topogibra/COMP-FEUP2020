import Types.NodeName;
import Types.VarTypes;

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
            if (exception.isError()) {
                System.err.println(exception.getMessage());
                no_error++;
                if (no_error >= MAX_NUM_ERRORS) {
                    throw new Exception("Reached max number of semantic errors");
                }
            } else
                System.out.println(exception.getMessage());
        }
    }

    public void startAnalyse() throws Exception {
        this.analyse();

        if (this.no_error > 0) {
            throw new Exception("Found " + this.no_error + " semantic errors");
        }
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
                case NodeName.IF:
                case NodeName.WHILE: {
                    String conditionType = this.analyseExpression(child.getChild(0), functionDescriptor);
                    if (conditionType != null)
                        if (!conditionType.equals(VarTypes.BOOLEAN))
                            addException(new SemanticException(child.getChild(0))); //TODO contition not boolean
                    break;
                }
                case NodeName.RETURN: {
                    String returnType = this.analyseExpression(child.getChild(0), functionDescriptor);
                    if (returnType != null)
                        if (!returnType.equals(functionDescriptor.getReturnType()))
                            addException(new SemanticException(child.getChild(0))); //TODO expression return type not equal to function return type
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
            addException(new SemanticException(firstChild)); //TODO method called on primitive type (ADD array type)
            return null;
        }

        if (Utils.isClassVariable(symbolTables, firstChild, functionDescriptor)) { // [ClassName | new ClassName].method
            if (secondChildName.equals(NodeName.METHODCALL)) {
                String returnType = getMethodReturnType(secondChild, functionDescriptor);
                if (returnType == null)
                    addException(new SemanticException(dotMethodNode)); // TODO method not found
                else
                    return returnType;
            }
        } else { // Call a method from an import
            ImportDescriptor importDescriptor = Utils.getImportedMethod(symbolTables, dotMethodNode, functionDescriptor);
            if (importDescriptor == null) {
                if (secondChildName.equals(NodeName.LENGTH)) {
                    if (this.analyseExpression(firstChild, functionDescriptor).equals(VarTypes.INTARRAY))
                        return VarTypes.INT;
                    else
                        this.addException(new SemanticException(dotMethodNode)); //TODO ATRIBUTO LENGTH NÃO EXISTE
                }
                else
                    this.addException(new SemanticException(dotMethodNode)); // TODO: Metodo não encontrado
            }
            else
                return importDescriptor.getReturnType().getTypeIdentifier();
        }

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
            for (Node grandchildNode : secondChild.jjtGetChildren()) {
                SimpleNode argNode = (SimpleNode) grandchildNode;

                String type = this.analyseExpression(argNode, functionDescriptor);
                if (type != null)
                    methodIdentifier.append(type);
            }
        }

        return methodIdentifier.toString();
    }

    private String analyseArithmeticExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {
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
                    return null;
                }

                return VarTypes.BOOLEAN;
            }
        }

        addException(new SemanticException(expressionNode)); // TODO Make the exception more specific
        return null;
    }

    private boolean isInteger(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String type = this.analyseExpression(simpleNode, functionDescriptor, ignore_init);
        return type != null && type.equals(VarTypes.INT);
    }

    private boolean isBoolean(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        String type = this.analyseExpression(simpleNode, functionDescriptor, ignore_init);
        return type != null && type.equals(VarTypes.BOOLEAN);
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

        if (Utils.isArithmeticExpression(rightSide)) {
            String rightType = analyseArithmeticExpression(rightSide, functionDescriptor);
            if (!leftType.equals(rightType)) {
                addException(new NotSameType(simpleNode, leftType, rightType));
                return;
            }

            functionDescriptor.getScope().setInit(leftSide.jjtGetVal(), true);
            return;
        }

        String rightType = this.analyseExpression(rightSide, functionDescriptor);
        if (rightType != null) {
            if (!leftType.equals(rightType))
                this.addException(new NotSameType(simpleNode, leftType, rightType));
        }

        functionDescriptor.getScope().setInit(leftSide.jjtGetVal(), true);
    }

    private String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {
        return this.analyseExpression(expressionNode, functionDescriptor, false);
    }

    private String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {

        switch (expressionNode.getNodeName()) {
            case NodeName.ARRAYACCESS: {
                if (this.analyseArray(false, expressionNode, functionDescriptor))
                    return VarTypes.INT;
                break;
            }
            case NodeName.DOTMETHOD: {
                return this.analyseDotMethod(expressionNode, functionDescriptor);
            }
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(expressionNode.jjtGetVal());
                if (!ignore_init) {
                    if (typeDescriptor == null) {
                        this.addException(new SemanticException(expressionNode));
                        return null;
                    }
                    if (!typeDescriptor.isInit())
                        this.addException(new VarNotInitialized(expressionNode));
                    return typeDescriptor.getTypeIdentifier();
                }
                break;
            }
            case NodeName.NEW: {
                SimpleNode childNode = (SimpleNode) expressionNode.jjtGetChild(0);
                switch (childNode.getNodeName()) {
                    case NodeName.ARRAYSIZE: { // new int[1]
                        if (!analyseArray(true, childNode, functionDescriptor)) {
                            this.addException(new SemanticException(childNode));
                            return null;
                        } else
                            return VarTypes.INTARRAY;
                    }
                    case NodeName.IDENTIFIER: { // new ClassName();
                        if (!symbolTables.getClassName().equals(childNode.jjtGetVal())) {
                            addException(new SemanticException(childNode)); // TODO Make the exception more specific
                            return null;
                        } else return expressionNode.jjtGetVal();
                    }
                }
                break;
            }
            case NodeName.BOOLEAN: {
                return VarTypes.BOOLEAN;
            }
            case NodeName.INT: {
                return VarTypes.INT;
            }
            case NodeName.THIS: {
                return this.symbolTables.getClassName();
            }
            default: {
                if (Utils.isArithmeticExpression(expressionNode))
                    return this.analyseArithmeticExpression(expressionNode, functionDescriptor);
                break;
            }
        }

        return null;
    }
}
