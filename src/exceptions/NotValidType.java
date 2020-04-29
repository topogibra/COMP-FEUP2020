package exceptions;

import parser.SimpleNode;

public class NotValidType extends SemanticException {
    public NotValidType(SimpleNode simpleNode) {
        super(simpleNode);
    }
}
