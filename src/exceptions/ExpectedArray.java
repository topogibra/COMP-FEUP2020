package exceptions;

import parser.SimpleNode;

public class ExpectedArray extends SemanticException {
    public ExpectedArray(SimpleNode simpleNode, String typeGiven) {
        super(simpleNode, "was expecting an array, given " + typeGiven);
    }
}
