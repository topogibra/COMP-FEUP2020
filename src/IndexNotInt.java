import Types.VarTypes;

public class IndexNotInt extends SemanticException {
    public IndexNotInt(SimpleNode simpleNode) {
        super(simpleNode, "index type expected to be " + VarTypes.INT + " given " + simpleNode.jjtGetVal());
    }
}
