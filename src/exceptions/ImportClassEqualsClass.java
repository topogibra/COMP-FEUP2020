package exceptions;

import parser.SimpleNode;

public class ImportClassEqualsClass extends SemanticException {
    public ImportClassEqualsClass(SimpleNode node) {
        super(node, "Cannot import own class", true);
    }
}
