package exceptions;

import parser.SimpleNode;

public class UnexpectedType extends SemanticException {
    public UnexpectedType(SimpleNode node, String expected, String given) {
        super(node, "expected " + expected + " given " + given,true  );
    }

    public UnexpectedType(SimpleNode node, String expected) {
        super(node, "expected " + expected ,true  );
    }
}
