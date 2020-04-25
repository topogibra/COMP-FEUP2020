public class ExpectedArray extends SemanticException {
    public ExpectedArray(SimpleNode simpleNode,String typeGiven) {
        super(simpleNode, simpleNode.jjtGetVal() + " is " + typeGiven + " not array");
    }
}
