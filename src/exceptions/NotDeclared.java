package exceptions;

import parser.SimpleNode;

public class NotDeclared extends SemanticException {
    public NotDeclared(SimpleNode simpleNode) {
        super(simpleNode,"variable " + simpleNode.jjtGetVal() + " not declared");
    }
}
