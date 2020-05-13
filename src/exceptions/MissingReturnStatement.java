package exceptions;

import parser.SimpleNode;

public class MissingReturnStatement extends SemanticException {
    public MissingReturnStatement(SimpleNode lastStatement, String returnType) {
        super(lastStatement,"reached end of function without return statement (" + returnType + ")");
    }
}
