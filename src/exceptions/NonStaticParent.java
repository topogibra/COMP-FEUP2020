package exceptions;

import parser.SimpleNode;

public class NonStaticParent extends SemanticException {
    public NonStaticParent(SimpleNode node) {
        super(node, "Static import of class", true);
    }
}
