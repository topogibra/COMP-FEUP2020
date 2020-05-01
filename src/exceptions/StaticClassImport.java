package exceptions;

import parser.SimpleNode;

public class StaticClassImport extends SemanticException {
    public StaticClassImport(SimpleNode node) {
        super(node, "static class import", true);
    }
}
