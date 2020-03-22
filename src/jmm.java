import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class jmm {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java Parser <file_name>");
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

        Parser parser = new Parser(input);

        try {
            parser.parseProgram(args[0]);
        } catch (Exception e) {
            throw new Exception();
        }
    }
}
