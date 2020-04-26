import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
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
        this.write("\n");
        this.generateFields(symbolTables);
        this.write("\n");
        this.generateMethods(symbolTables);
        this.write("\n");

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

        stringBuilder.append("\n");

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

    public void generateMethods(SymbolTables symbolTables) throws IOException {
        for (Map.Entry<String, FunctionDescriptor> method : symbolTables.getMethods().entrySet()) {
            this.generateMethod(symbolTables, method.getValue());
            this.write("\n");
        }
    }

    public void generateMethod(SymbolTables symbolTables, FunctionDescriptor functionDescriptor) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        if (functionDescriptor.getMethodNode() == null)
            return;

        stringBuilder.append(".method public ");

        //Method header
        if (functionDescriptor.getMethodName().equals("main"))
            stringBuilder.append("static main([Ljava/lang/String;)V");
        else {
            stringBuilder.append(functionDescriptor.getMethodName());
            stringBuilder.append("(");

            //Method parameters
            if (functionDescriptor.getParams().size() > 0) {
                for (Map.Entry<String, TypeDescriptor> param : functionDescriptor.getParams().entrySet()) {
                    stringBuilder.append(param.getValue().toJVM());
                    stringBuilder.append(",");
                }

                stringBuilder.setLength(stringBuilder.length() - 1);
            }
            stringBuilder.append(")");
            stringBuilder.append(TypeDescriptor.toJVM(functionDescriptor.getReturnType()));
        }

        stringBuilder.append("\n");
        stringBuilder.append(".end method\n");
        this.write(stringBuilder.toString());
    }
}

