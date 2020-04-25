import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class jmm {

    public static String filepath;

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

        jmm.filepath = args[0];

        Parser parser = new Parser(input);

        SimpleNode root = null;
        try {
            root = parser.parseProgram(args[0]);
            root.dump("");
            SemanticAnalyser.analyse(SymbolTablesGenerator.generate(root), root);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception();
        }

    }

}
