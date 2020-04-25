import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class SemanticException extends Exception {
    public SimpleNode simpleNode;
    private String messages;

    public SemanticException(SimpleNode simpleNode) {
        super();
        this.simpleNode = simpleNode;
        this.messages = simpleNode.toString() + " Line " + simpleNode.jjtGetFirstToken().beginLine + " Column " + simpleNode.jjtGetFirstToken().beginColumn;

        //simpleNode.getParent().dump(" ");
    }

    public SemanticException(SimpleNode simpleNode,String errormessage) {
        super();
        SimpleNode tmp = (simpleNode.getParent() == null) ? simpleNode : simpleNode.getParent();
        this.simpleNode = simpleNode;
        this.messages = printTokenErrorMessage(tmp.jjtGetFirstToken(),tmp.jjtGetLastToken(),errormessage);


        //simpleNode.getParent().dump(" ");
    }

    @Override
    public String getMessage() {
        return this.messages;
    }

    public String printTokenErrorMessage(Token firstToken, Token lastToken, String message) {
        int line = firstToken.beginLine;
        int col = firstToken.beginColumn;
        String path = jmm.filepath.toAbsolutePath().toString();
        String classname = this.getClass().toString().substring(6);
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append(path).append(":");
        errorMessage.append(line);
        errorMessage.append(": error: ");
        errorMessage.append(classname).append(": \n");

        try {
            FileInputStream fileStream = new FileInputStream(path);
            InputStreamReader inputStream = new InputStreamReader(fileStream);
            BufferedReader reader = new BufferedReader(inputStream);

            for (int i = 0; i < line - 1; i++)
                reader.readLine();

            errorMessage.append(reader.readLine()).append("\n");
            errorMessage.replace(0,errorMessage.length(),errorMessage.toString().replaceAll("\t", " "));

        } catch(Exception e) {
            System.out.println("File not Found.");
            return "";
        }

        for (int i = 0; i < col - 1; i++)
            errorMessage.append(" ");

        for (int i=0; i < Math.abs(col - lastToken.endColumn - 1); i++){
            errorMessage.append("^");
        }

        return errorMessage.toString();
    }
}
