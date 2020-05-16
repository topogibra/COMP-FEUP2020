package exceptions;

import parser.SimpleNode;

public class ExtendedClassEqualsClass extends SemanticException {
    public ExtendedClassEqualsClass(SimpleNode child) {
        super(child, "Cannot extend own class", true);
    }
}
