package main;

import Types.NodeName;
import Types.VarTypes;
import exceptions.*;
import parser.Node;
import parser.ParserTreeConstants;
import parser.SimpleNode;

public class SemanticAnalyser {
    private static final int MAX_NUM_ERRORS = 10;
    private static int no_error = 0;

    private final SymbolTables symbolTables;
    private final SimpleNode root;
    public static boolean ignore_exceptions;

    public SemanticAnalyser(SymbolTables symbolTables, SimpleNode root, boolean ignore_exceptions) {
        this.symbolTables = symbolTables;
        this.root = root;
        SemanticAnalyser.no_error = 0;
    }

    public static void addException(SemanticException exception) throws Exception {
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

        if (no_error > 0) {
            throw new Exception("Found " + no_error + " semantic errors");
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
                        addException(new ExtendedClassNotImported(child));
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
                if (!symbolTables.getClassName().equals(type) && !symbolTables.isImportedClass(type)) {
                    addException(new ClassNotImported(varDeclarationNode,type));
                }
                break;
            }
        }
    }

    private boolean canUseThis(SimpleNode dotmethod,FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = dotmethod.getChild(0);
        boolean canUse = firstChild.getNodeName().equals(NodeName.THIS) || (firstChild.getNodeName().equals(NodeName.DOTMETHOD) && canUseThis(firstChild,functionDescriptor));


        if(canUse && firstChild.getNodeName().equals(NodeName.THIS) && functionDescriptor.isMain()){ // this in static context
            addException(new ThisFromStaticContext(firstChild));
            return false;
        }
        return true;
    }

    private void analyseMethodBody(SimpleNode methodBodyNode, FunctionDescriptor functionDescriptor) throws Exception {
        Node[] children = methodBodyNode.jjtGetChildren();

        if (children == null)
            return;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;
            String childName = ParserTreeConstants.jjtNodeName[child.getId()];

            switch (childName) {
                case NodeName.DOTMETHOD:
                    if(!canUseThis(child,functionDescriptor)){
                        continue;
                    }
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
                            addException(new ConditionNotBoolean(child.getChild(0), conditionType));
                    break;
                }
                case NodeName.RETURN: {
                    String returnType = this.analyseExpression(child.getChild(0), functionDescriptor);
                    if (returnType != null)
                        if (!returnType.equals(functionDescriptor.getReturnType()))
                            addException(new ReturnNotSameType(child.getChild(0), functionDescriptor.getReturnType(), returnType));
                    break;
                }
                default: {
                    addException(new NotStatement(child));
                    break;
                }
            }
        }
    }


    private boolean analyseArray(boolean isArraySize, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
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

        // Analyses if value inside array brackets is a positive integer
        if (!isInteger(analysedChild, functionDescriptor)) {
            addException(new IndexNotInt(analysedChild));
            return false;
        }

        return true;
    }

    public String analyseDotMethod(SimpleNode dotMethodNode, FunctionDescriptor functionDescriptor) throws Exception {
        return analyseDotMethod(dotMethodNode, functionDescriptor, false);
    }

    public String analyseDotMethod(SimpleNode dotMethodNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        SimpleNode firstChild = (SimpleNode) dotMethodNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) dotMethodNode.jjtGetChildren()[1];

        String secondChildName = ParserTreeConstants.jjtNodeName[secondChild.getId()];

        // Call a method from an import
        ImportDescriptor importDescriptor = Utils.getImportedMethod(symbolTables, dotMethodNode, functionDescriptor);
        if (importDescriptor != null)
            return importDescriptor.getReturnType().getTypeIdentifier();

        if (isInteger(firstChild, functionDescriptor, ignore_init) || isBoolean(firstChild, functionDescriptor, ignore_init)) {
            //TODO Add array type
            addException(new MethodCallOnPrimitive(firstChild));
            return null;
        }

        if (Utils.isClassVariable(symbolTables, firstChild, functionDescriptor)) { // [ClassName | new ClassName].method
            if (secondChildName.equals(NodeName.METHODCALL)) {
                String returnType = getMethodReturnType(secondChild, functionDescriptor);
                if (returnType == null)
                    addException(new MethodNotFound(dotMethodNode));
                else
                    return returnType;
            } else {
                //this.length
                addException(new AttributeDoesNotExist(dotMethodNode));
            }
        }
        else if (secondChildName.equals(NodeName.LENGTH)) {
            if (this.analyseExpression(firstChild, functionDescriptor).equals(VarTypes.INTARRAY))
                return VarTypes.INT;
            else
                addException(new AttributeDoesNotExist(dotMethodNode)); //TODO: TEST
        }
        else
            addException(new MethodNotFound(dotMethodNode));

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
        methodIdentifier.append(firstChild.jjtGetVal());

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
        boolean stop = false;

        switch (nodeName) {
            case NodeName.SUB:
            case NodeName.ADD:
            case NodeName.MUL:
            case NodeName.DIV:
            case NodeName.LESS: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isInteger(firstChild, functionDescriptor)) {
                    addException(new UnexpectedType(firstChild, VarTypes.INT));
                    stop = true;
                }

                if (!isInteger(secondChild, functionDescriptor)) {
                    addException(new UnexpectedType(secondChild, VarTypes.INT));
                    stop = true;
                }



                return (stop) ? null : nodeName.equals(NodeName.LESS) ? VarTypes.BOOLEAN : VarTypes.INT;
            }
            case NodeName.AND: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isBoolean(firstChild, functionDescriptor)) {
                    addException(new UnexpectedType(firstChild, VarTypes.BOOLEAN));
                    stop = true;
                }

                if (!isBoolean(secondChild, functionDescriptor)) {
                    addException(new UnexpectedType(secondChild, VarTypes.BOOLEAN));
                    stop = true;
                }

                return (stop) ? null : VarTypes.BOOLEAN;
            }
            case NodeName.NOT: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];

                if (!isBoolean(firstChild, functionDescriptor)) {
                    addException(new UnexpectedType(firstChild, VarTypes.BOOLEAN));
                    return null;
                }

                return VarTypes.BOOLEAN;
            }
        }

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
                return;
            }
        }

        if(rightSide.getNodeName().equals(NodeName.DOTMETHOD) && !canUseThis(rightSide,functionDescriptor)){
            return;
        }

        String rightType = this.analyseExpression(rightSide, functionDescriptor);
        if (rightType != null) {
            if (!leftType.equals(rightType))
                addException(new NotSameType(simpleNode, leftType, rightType));
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
                        addException(new NotDeclared(expressionNode));
                        return null;
                    }
                    if (!typeDescriptor.isInit())
                        addException(new VarNotInitialized(expressionNode));
                    return typeDescriptor.getTypeIdentifier();
                }
                break;
            }
            case NodeName.NEW: {
                SimpleNode childNode = (SimpleNode) expressionNode.jjtGetChild(0);
                switch (childNode.getNodeName()) {
                    case NodeName.ARRAYSIZE: { // new int[1]
                        if (!analyseArray(true, childNode, functionDescriptor)) {
                            addException(new SemanticException(childNode));
                            return null;
                        } else
                            return VarTypes.INTARRAY;
                    }
                    case NodeName.IDENTIFIER: { // new ClassName();
                        if (!symbolTables.getClassName().equals(childNode.jjtGetVal()) && !symbolTables.isImportedClass(childNode.jjtGetVal())) {
                            addException(new ClassNotImported(childNode,childNode.jjtGetVal()));
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
