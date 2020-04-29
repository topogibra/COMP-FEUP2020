public class MethodCallOnPrimitive extends SemanticException {

    public MethodCallOnPrimitive(SimpleNode simpleNode) {
        super(simpleNode, "method called on primitive type", true);
    }
}
