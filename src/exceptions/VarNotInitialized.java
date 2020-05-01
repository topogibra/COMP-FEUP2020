package exceptions;

import parser.SimpleNode;

public class VarNotInitialized extends SemanticException {
    public VarNotInitialized(SimpleNode simpleNode) {
        super(simpleNode, "var " + simpleNode.jjtGetVal() + " might not have been initialized", true);
    }
}
