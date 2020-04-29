public class NotStatement extends SemanticException {

    public NotStatement(SimpleNode simpleNode) {
        super(simpleNode, "Not a valid statement", true);
    }
}
