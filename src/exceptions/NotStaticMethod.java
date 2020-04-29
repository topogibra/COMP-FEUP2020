package exceptions;

import parser.SimpleNode;

public class NotStaticMethod extends SemanticException {
    public NotStaticMethod(SimpleNode classnameNode) {
        super(classnameNode.getParent(),"called non-static method from non instantiated object " + classnameNode.jjtGetVal(),true);
    }
}
