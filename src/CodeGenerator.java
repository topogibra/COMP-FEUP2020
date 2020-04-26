import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class CodeGenerator {
    private Path path;
    private final String FILE_EXTENSION = ".j";

    private void createFile(String className) {
        this.path = Paths.get(className + FILE_EXTENSION);

        try {
            if (!Files.exists(this.path))
                Files.createFile(this.path);
            else
                Files.write(this.path, "".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generate(SymbolTables symbolTables, SimpleNode rootNode) throws IOException {
        this.createFile(symbolTables.getClassName());

        this.generateClass(symbolTables);

        this.generateFields(symbolTables);


    }

    public void write(String string) throws IOException {
        Files.write(this.path, string.getBytes(), StandardOpenOption.APPEND);
    }

    public void generateClass(SymbolTables symbolTables) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(".class public ").append(symbolTables.getClassName()).append("\n");

        stringBuilder.append(".super ");
        if (symbolTables.getExtendedClassName() != null)
            stringBuilder.append(symbolTables.getExtendedClassName());
        else
            stringBuilder.append("java/lang/Object");

        stringBuilder.append("\n\n");

        this.write(stringBuilder.toString());
    }

    public void generateFields(SymbolTables symbolTables) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, TypeDescriptor> field : symbolTables.getScope().getVars().entrySet()) {
            stringBuilder.append(".field public ");
            stringBuilder.append(field.getKey()).append(" ");
            stringBuilder.append(field.getValue().toJVM()).append("\n");
        }

        this.write(stringBuilder.toString());
    }
}
