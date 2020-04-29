package exceptions;

import parser.SimpleNode;

public class ClassNotImported extends SemanticException {
    public ClassNotImported(SimpleNode childNode,String classImported) {
        super(childNode,"class " + classImported + " not imported",true);
    }
}
