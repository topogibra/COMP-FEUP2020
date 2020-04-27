import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class jmm {

    public static Path filepath;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java jmm <file_name>");
            return;
        }
        //Reading file with InputStream
        //The file is passed as argument (1st argument)
        File file = new File(args[0]);
        InputStream input;

        try {
            input = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("File not Found.");
            return;
        }

        jmm.filepath = Paths.get(args[0]);

        Parser parser = new Parser(input);

        SimpleNode root = null;

        CodeGenerator codeGenerator;
        try {
            root = parser.parseProgram(args[0]);
            root.dump("");
            SymbolTables symbolTables = SemanticAnalyser.startAnalyse(root);
            codeGenerator = new CodeGenerator(symbolTables);
            codeGenerator.generate(root);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new Exception();
        }

    }

}
