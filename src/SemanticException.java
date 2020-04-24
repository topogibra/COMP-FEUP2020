public class SemanticException extends Exception {
    public int beginLine;
    public int beginColumn;
    public int endLine;
    public int endColumn;
    public SemanticException(SimpleNode simpleNode) {
        super(simpleNode.toString() + " Line " + simpleNode.jjtGetFirstToken().beginLine + " Column " + simpleNode.jjtGetFirstToken().beginColumn );
        this.endColumn = simpleNode.jjtGetLastToken().endColumn;
        this.beginColumn = simpleNode.jjtGetFirstToken().beginColumn;
        this.beginLine = simpleNode.jjtGetFirstToken().beginLine;
        this.endLine = simpleNode.jjtGetLastToken().endLine;
        //simpleNode.getParent().dump(" ");
    }
}
