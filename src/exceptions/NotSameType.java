package exceptions;

import parser.SimpleNode;

public class NotSameType extends SemanticException {
    public NotSameType(SimpleNode simpleNode, String expectedType, String wrongType) {
        super(simpleNode,"expected "  + expectedType + " given " + wrongType);
    }
}
