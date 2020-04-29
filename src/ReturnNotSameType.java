public class ReturnNotSameType extends SemanticException {

    public ReturnNotSameType(SimpleNode simpleNode, String functionReturnType, String givenType) {
        super(simpleNode, "function return type is " + functionReturnType + ", given " + givenType, true);
    }
}
