public class ArrayAccessOutOfBounds extends SemanticException {
    public ArrayAccessOutOfBounds(SimpleNode arrayAccessNode) {
        super(arrayAccessNode);
    }
}
