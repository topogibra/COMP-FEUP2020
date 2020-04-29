package exceptions;

import parser.SimpleNode;

public class ExtendedClassNotImported extends SemanticException {

    public ExtendedClassNotImported(SimpleNode simpleNode) {
        super(simpleNode, "extended class not found", true);
    }
}
