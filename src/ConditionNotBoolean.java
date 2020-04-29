public class ConditionNotBoolean extends SemanticException {
    public ConditionNotBoolean(SimpleNode simpleNode, String givenType) {
        super(simpleNode, "condition expression does not return boolean, returns " + givenType , true);
    }
}
