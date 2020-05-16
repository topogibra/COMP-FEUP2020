package main;

import Types.NodeName;
import Types.VarTypes;
import exceptions.*;
import parser.Node;
import parser.ParserTreeConstants;
import parser.SimpleNode;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class SemanticAnalyser {
    private static final int MAX_NUM_ERRORS = 10;
    private int no_error ;

    private final static int BYTE_SIZE = 127;
    public final static int SHORT_SIZE = 32767;
    private final static int LONG_SIZE = 2147483647;

    private final SymbolTables symbolTables;
    private final SimpleNode root;
    public boolean ignore_exceptions;

    public SemanticAnalyser(SymbolTables symbolTables, SimpleNode root, boolean ignore_exceptions) {
        this.symbolTables = symbolTables;
        this.root = root;
        this.no_error = 0;
    }

    public void addException(SemanticException exception) throws Exception {
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

        this.analyseImports();

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;

            if (child.getNodeName().equals(NodeName.CLASS)) {
                this.analyseClass(child);
                break;
            }
        }
    }

    private void analyseImports() throws Exception {
        for (Map.Entry<String, ImportDescriptor> entry : this.symbolTables.getImports().entrySet()) {
            ImportDescriptor importDescriptor = entry.getValue();

            if (importDescriptor.isStatic() && importDescriptor.getMethodName() == null)
                this.addException(new StaticClassImport(importDescriptor.getNode()));

            if (importDescriptor.getClassName().equals(symbolTables.getClassName()))
                this.addException(new ImportClassEqualsClass(importDescriptor.getNode()));
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
                    if (extendedClassName.equals(symbolTables.getClassName())) {
                        addException(new ExtendedClassEqualsClass(child));
                        return;
                    }
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
                this.analyseStatements(child, functionDescriptor);
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
                    addException(new ClassNotImported(varDeclarationNode, type));
                }
                break;
            }
        }
    }

    private boolean canUseThis(SimpleNode dotmethod, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = dotmethod.getChild(0);
        boolean canUse = firstChild.getNodeName().equals(NodeName.THIS) || (firstChild.getNodeName().equals(NodeName.DOTMETHOD) && canUseThis(firstChild, functionDescriptor));


        if (canUse && firstChild.getNodeName().equals(NodeName.THIS) && functionDescriptor.isMain()) { // this in static context
            addException(new ThisFromStaticContext(firstChild));
            return false;
        }
        return true;
    }

    private Set<String> analyseStatements(SimpleNode methodBodyNode, FunctionDescriptor functionDescriptor) throws Exception {
        return analyseStatements(methodBodyNode, functionDescriptor, null);
    }

    private Set<String> analyseStatements(SimpleNode methodBodyNode, FunctionDescriptor functionDescriptor, Set<String> scopeVars) throws Exception {
        Node[] children = methodBodyNode.jjtGetChildren();
        Set<String> result = new HashSet<>();
        if (scopeVars != null)
            result.addAll(scopeVars);

        if (children == null)
            return result;

        for (Node node : children) {
            SimpleNode child = (SimpleNode) node;
            String childName = ParserTreeConstants.jjtNodeName[child.getId()];

            switch (childName) {
                case NodeName.DOTMETHOD:
                    if (!canUseThis(child, functionDescriptor)) {
                        continue;
                    }
                    this.analyseDotMethod(child, functionDescriptor, false, result);
                    break;
                case NodeName.ASSIGNMENT: // a = 1;
                    boolean inited = this.analyseAssignment(child, functionDescriptor, result);
                    if (inited) {
                        SimpleNode leftSide = child.getChild(0);
                        if (leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
                            String td = leftSide.jjtGetVal();
                            result.add(td);
                        }
                    }
                    break;
                case NodeName.VARDECLARATION: {
                    this.analyseVarDeclaration(child);
                    break;
                }
                case NodeName.IF:
                case NodeName.WHILE: {
                    String conditionType = this.analyseExpression(child.getChild(0), functionDescriptor, false, result);
                    if (conditionType != null) {
                        if (!conditionType.equals(VarTypes.BOOLEAN))
                            addException(new ConditionNotBoolean(child.getChild(0), conditionType));
                    }
                    Set<String> varsInitIf = this.analyseStatements(child.getChild(1), functionDescriptor, result);
                    if (childName.equals(NodeName.IF)) {
                        // Analyse else block
                        Set<String> varsInitElse = this.analyseStatements(child.getChild(2), functionDescriptor, result);

                        varsInitIf.retainAll(varsInitElse);
                        result.addAll(varsInitIf);
                        if (child.getParent().getNodeName().equals(NodeName.METHODBODY)) {
                            for (String s : varsInitIf) {
                                TypeDescriptor td = functionDescriptor.getTypeDescriptor(s);
                                if (td != null)
                                    td.setInit(true);
                            }
                        }
                    }
                    break;
                }
                case NodeName.RETURN: {
                    if (child.jjtGetNumChildren() == 0) {
                        if (!functionDescriptor.getReturnType().equals(VarTypes.VOID)) {
                            addException(new ReturnNotSameType(child, functionDescriptor.getReturnType(), VarTypes.VOID));
                        }
                        break;
                    }
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
        if (methodBodyNode.getNodeName().equals(NodeName.METHODBODY)) {
            SimpleNode lastStatement = methodBodyNode.getChild(methodBodyNode.jjtGetNumChildren() - 1);
            if (!lastStatement.getNodeName().equals(NodeName.RETURN) && !functionDescriptor.getReturnType().equals(VarTypes.VOID)) {
                addException(new MissingReturnStatement(lastStatement,functionDescriptor.getReturnType()));
            }
        }
        return result;
    }


    private boolean analyseArray(boolean isArraySize, SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        SimpleNode firstChild = (SimpleNode) simpleNode.jjtGetChildren()[0];

        if (!isArraySize) { //Check if it is an array that's being accessed
            if (firstChild.getNodeName().equals(NodeName.IDENTIFIER)) {
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
            else if (firstChild.getNodeName().equals(NodeName.DOTMETHOD)) {
                String returnType = this.analyseDotMethod(firstChild, functionDescriptor);
                if (!returnType.equals(VarTypes.INTARRAY)) {
                    addException(new ExpectedArray(firstChild, returnType));
                    return false;
                }
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
        return analyseDotMethod(dotMethodNode, functionDescriptor, false, new HashSet<>());
    }

    public String analyseDotMethod(SimpleNode dotMethodNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        return analyseDotMethod(dotMethodNode, functionDescriptor, ignore_init, new HashSet<>());
    }

    public String analyseDotMethod(SimpleNode dotMethodNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInitScope) throws Exception {
        SimpleNode firstChild = (SimpleNode) dotMethodNode.jjtGetChildren()[0];
        SimpleNode secondChild = (SimpleNode) dotMethodNode.jjtGetChildren()[1];

        String secondChildName = ParserTreeConstants.jjtNodeName[secondChild.getId()];

        // Call a method from an import
        ImportDescriptor importDescriptor = Utils.getImportedMethod(symbolTables, dotMethodNode, functionDescriptor, varInitScope);
        if (importDescriptor != null) {
            if (firstChild != null && firstChild.jjtGetVal() != null && !firstChild.jjtGetVal().equals(NodeName.DOTMETHOD)) {
                if (!importDescriptor.isStatic() && firstChild.jjtGetVal().equals(importDescriptor.getClassName())) {
                    addException(new NotStaticMethod(firstChild));
                }
            }
            return importDescriptor.getReturnType().getTypeIdentifier();
        }

        if (Utils.isClassVariable(symbolTables, firstChild, functionDescriptor)) { // [this | ClassName | new ClassName].method
            if (secondChildName.equals(NodeName.METHODCALL)) {
                String returnType = getMethodReturnType(secondChild, functionDescriptor, varInitScope);
                if (returnType == null)
                    addException(new MethodNotFound(dotMethodNode));
                else
                    return returnType;
            } else {
                //this.length
                addException(new AttributeDoesNotExist(dotMethodNode));
            }
        } else if (secondChildName.equals(NodeName.LENGTH)) {
            if (this.analyseExpression(firstChild, functionDescriptor).equals(VarTypes.INTARRAY))
                return VarTypes.INT;
            else
                addException(new AttributeDoesNotExist(dotMethodNode));
        } else if (isPrimitiveType(firstChild, functionDescriptor, ignore_init)) {
            addException(new MethodCallOnPrimitive(firstChild));
            return null;
        } else
            addException(new MethodNotFound(dotMethodNode));

        return null;
    }

    private String getMethodReturnType(SimpleNode methodCallNode, FunctionDescriptor functionDescriptor, Set<String> varInitScope) throws Exception {
        String methodIdentifier = parseMethodIdentifier(methodCallNode, functionDescriptor, varInitScope);
        FunctionDescriptor methodDescriptor = symbolTables.getFunctionDescriptor(methodIdentifier);
        return (methodDescriptor == null) ? null : methodDescriptor.getReturnType();
    }

    public String parseMethodIdentifier(SimpleNode methodCallNode, FunctionDescriptor functionDescriptor) throws Exception {
        return parseMethodIdentifier(methodCallNode, functionDescriptor, new HashSet<>());
    }

    public String parseMethodIdentifier(SimpleNode methodCallNode, FunctionDescriptor functionDescriptor, Set<String> varInitScope) throws Exception {
        StringBuilder methodIdentifier = new StringBuilder();

        if (methodCallNode.jjtGetChildren() == null || methodCallNode.jjtGetChildren().length == 0)
            return null;

        SimpleNode firstChild = (SimpleNode) methodCallNode.jjtGetChildren()[0];
        methodIdentifier.append(firstChild.jjtGetVal()).append("&");

        if (methodCallNode.jjtGetChildren().length > 1) { //Check if method call has arguments
            SimpleNode secondChild = (SimpleNode) methodCallNode.jjtGetChildren()[1];
            for (Node grandchildNode : secondChild.jjtGetChildren()) {
                SimpleNode argNode = (SimpleNode) grandchildNode;

                String type = this.analyseExpression(argNode, functionDescriptor, false, varInitScope);
                if (type != null)
                    methodIdentifier.append(type);
            }
        }

        return methodIdentifier.toString();
    }

    private String analyseArithmeticExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {
        return analyseArithmeticExpression(expressionNode, functionDescriptor, new HashSet<>());
    }

    private String analyseArithmeticExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor, Set<String> varInitScope) throws Exception {
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

                if (!isInteger(firstChild, functionDescriptor, false, varInitScope)) {
                    addException(new UnexpectedType(firstChild, VarTypes.INT));
                    stop = true;
                }

                if (!isInteger(secondChild, functionDescriptor, false, varInitScope)) {
                    addException(new UnexpectedType(secondChild, VarTypes.INT));
                    stop = true;
                }


                return (stop) ? null : nodeName.equals(NodeName.LESS) ? VarTypes.BOOLEAN : VarTypes.INT;
            }
            case NodeName.AND: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];
                SimpleNode secondChild = (SimpleNode) expressionNode.jjtGetChildren()[1];

                if (!isBoolean(firstChild, functionDescriptor, false, varInitScope)) {
                    addException(new UnexpectedType(firstChild, VarTypes.BOOLEAN));
                    stop = true;
                }

                if (!isBoolean(secondChild, functionDescriptor, false, varInitScope)) {
                    addException(new UnexpectedType(secondChild, VarTypes.BOOLEAN));
                    stop = true;
                }

                return (stop) ? null : VarTypes.BOOLEAN;
            }
            case NodeName.NOT: {
                SimpleNode firstChild = (SimpleNode) expressionNode.jjtGetChildren()[0];

                if (!isBoolean(firstChild, functionDescriptor, false, varInitScope)) {
                    addException(new UnexpectedType(firstChild, VarTypes.BOOLEAN));
                    return null;
                }

                return VarTypes.BOOLEAN;
            }
        }

        return null;
    }

    private boolean isPrimitiveType(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        return isInteger(simpleNode, functionDescriptor, ignore_init, new HashSet<>()) || isBoolean(simpleNode, functionDescriptor, ignore_init, new HashSet<>()) || isIntArray(simpleNode, functionDescriptor, ignore_init, new HashSet<>());
    }

    private boolean isPrimitiveType(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInitScope) throws Exception {
        return isInteger(simpleNode, functionDescriptor, ignore_init, varInitScope) || isBoolean(simpleNode, functionDescriptor, ignore_init, varInitScope) || isIntArray(simpleNode, functionDescriptor, ignore_init, varInitScope);
    }

    private boolean isInteger(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInitScope) throws Exception {
        String type = this.analyseExpression(simpleNode, functionDescriptor, ignore_init, varInitScope);
        return type != null && type.equals(VarTypes.INT);
    }

    private boolean isBoolean(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInitScope) throws Exception {
        String type = this.analyseExpression(simpleNode, functionDescriptor, ignore_init, varInitScope);
        return type != null && type.equals(VarTypes.BOOLEAN);
    }

    private boolean isIntArray(SimpleNode simpleNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInitScope) throws Exception {
        String type = this.analyseExpression(simpleNode, functionDescriptor, ignore_init, varInitScope);
        return type != null && type.equals(VarTypes.INTARRAY);
    }

    private boolean isInteger(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isInteger(simpleNode, functionDescriptor, false, new HashSet<>());
    }

    private boolean isBoolean(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isBoolean(simpleNode, functionDescriptor, false, new HashSet<>());
    }

    private boolean isIntArray(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isIntArray(simpleNode, functionDescriptor, false, new HashSet<>());
    }

    private boolean isPrimitiveType(SimpleNode simpleNode, FunctionDescriptor functionDescriptor) throws Exception {
        return isInteger(simpleNode, functionDescriptor) || isBoolean(simpleNode, functionDescriptor) || isIntArray(simpleNode, functionDescriptor);
    }

    private boolean analyseAssignment(SimpleNode assignmentNode, FunctionDescriptor functionDescriptor, Set<String> varInScope) throws Exception {
        SimpleNode leftSide = (SimpleNode) assignmentNode.jjtGetChildren()[0];
        SimpleNode rightSide = (SimpleNode) assignmentNode.jjtGetChildren()[1];

        String leftSideName = ParserTreeConstants.jjtNodeName[leftSide.getId()];
        String leftType;
        switch (leftSideName) {
            case NodeName.ARRAYACCESS: {
                if (!analyseArray(false, leftSide, functionDescriptor)) {
                    return false;
                }
                leftType = VarTypes.INT;
                break;
            }
            case NodeName.IDENTIFIER: {
                TypeDescriptor tmp = functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
                if (tmp == null) {
                    addException(new NotDeclared(leftSide));
                    return false;
                } else
                    leftType = tmp.getTypeIdentifier();
                break;
            }
            default: {
                return false;
            }
        }

        if (rightSide.getNodeName().equals(NodeName.DOTMETHOD) && !canUseThis(rightSide, functionDescriptor)) {
            return false;
        }

        String rightType = this.analyseExpression(rightSide, functionDescriptor, false, varInScope);
        if (rightType != null) {
            if (!leftType.equals(rightType))
                addException(new NotSameType(assignmentNode, leftType, rightType));
        }

        TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
        if (typeDescriptor == null)
            return false;

        if (!assignmentNode.getParent().getNodeName().equals(NodeName.IFBLOCK) && !assignmentNode.getParent().getNodeName().equals(NodeName.ELSE)) {
            typeDescriptor.setInit(true);
        }
        return true;
    }

    public String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor) throws Exception {
        Set<String> tmp = new HashSet<>();
        return this.analyseExpression(expressionNode, functionDescriptor, false, tmp);
    }

    public String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor, boolean ignore_init) throws Exception {
        Set<String> tmp = new HashSet<>();
        return this.analyseExpression(expressionNode, functionDescriptor, ignore_init, tmp);
    }


    public String analyseExpression(SimpleNode expressionNode, FunctionDescriptor functionDescriptor, boolean ignore_init, Set<String> varInScope) throws Exception {

        switch (expressionNode.getNodeName()) {
            case NodeName.ARRAYACCESS: {
                if (this.analyseArray(false, expressionNode, functionDescriptor))
                    return VarTypes.INT;
                break;
            }
            case NodeName.DOTMETHOD: {
                return this.analyseDotMethod(expressionNode, functionDescriptor, ignore_init, varInScope);
            }
            case NodeName.IDENTIFIER: {
                TypeDescriptor typeDescriptor = functionDescriptor.getTypeDescriptor(expressionNode.jjtGetVal());
                if (typeDescriptor == null) {
                    addException(new NotDeclared(expressionNode));
                    return null;
                }
                if (!ignore_init) {
                    if (!typeDescriptor.isInit()) // && nao estiver dentro da list com as variaveis inciadas neste scope
                        if (!(varInScope.contains(expressionNode.jjtGetVal())))
                            addException(new VarNotInitialized(expressionNode));
                }
                return typeDescriptor.getTypeIdentifier();
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
                        if (symbolTables.getClassName().equals(childNode.jjtGetVal()) || symbolTables.isImportedClass(childNode.jjtGetVal()))
                            return childNode.jjtGetVal();
                        else {
                            addException(new ClassNotImported(childNode, childNode.jjtGetVal()));
                            return null;
                        }
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
                    return this.analyseArithmeticExpression(expressionNode, functionDescriptor, varInScope);
                break;
            }
        }

        return null;
    }
}
