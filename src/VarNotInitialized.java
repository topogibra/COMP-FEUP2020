public class VarNotInitialized extends SemanticException {
    public VarNotInitialized(SimpleNode simpleNode) {
        super(simpleNode, "var " + simpleNode.jjtGetVal() + " not initialized");
    }
}
