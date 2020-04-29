public class MethodNotFound extends SemanticException {

    public MethodNotFound(SimpleNode simpleNode) {
        super(simpleNode, "method not found", true);
    }
}
