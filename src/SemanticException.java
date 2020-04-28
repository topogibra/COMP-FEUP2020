import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class SemanticException extends Exception {
    public final SimpleNode simpleNode;
    private final String messages;
    private final boolean is_error;

    public SemanticException(SimpleNode simpleNode) {
        this(simpleNode, "", true);
    }

    public SemanticException(SimpleNode simpleNode, String errormessage) {
        this(simpleNode, errormessage, true);
    }

    public SemanticException(SimpleNode simpleNode, String errormessage, boolean is_error) {
        super();
        this.simpleNode = simpleNode;
        this.is_error = is_error;
        this.messages = printTokenErrorMessage(simpleNode.jjtGetFirstToken(), simpleNode.jjtGetLastToken(),errormessage);
    }

    @Override
    public String getMessage() {
        return this.messages;
    }

    public String printTokenErrorMessage(Token firstToken, Token lastToken, String message) {
        StringBuilder errorMessage = new StringBuilder();

        int line = firstToken.beginLine;
        int col = firstToken.beginColumn;

        String path = jmm.filepath.toAbsolutePath().toString();
        String classname = this.getClass().toString().substring(6).toLowerCase();

        errorMessage.append(this.is_error ? "ERROR: " : "WARNING: ");
        errorMessage.append(path).append(":");
        errorMessage.append(line).append(" ");
        errorMessage.append(classname).append(": ").append(message.isEmpty() ? "" : message + ":").append("\n");

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

        errorMessage.append(" ".repeat(Math.max(0, col - 2)));

        for (int i=0; i < Math.abs(col - lastToken.endColumn); i++){
            errorMessage.append("^");
        }

        return errorMessage.toString();
    }

    public boolean isError() {
        return this.is_error;
    }
}
