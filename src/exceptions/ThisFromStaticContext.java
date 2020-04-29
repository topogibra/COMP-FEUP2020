package exceptions;

import parser.SimpleNode;

public class ThisFromStaticContext extends SemanticException {
    public ThisFromStaticContext(SimpleNode simpleNode) {
        super(simpleNode,"non-static variable this cannot be referenced from a static context",true);
    }
}
