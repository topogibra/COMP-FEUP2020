public class AttributeDoesNotExist extends SemanticException {

    public AttributeDoesNotExist(SimpleNode simpleNode) {
        super(simpleNode, "attribute length not found for given type", true);
    }
}
