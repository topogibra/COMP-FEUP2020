package main;

import parser.Parser;
import parser.SimpleNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class jmm {

    public static Path filepath;

    private boolean ignoreExceptions = false;

    Parser parser;
    SymbolTables symbolTables;
    SymbolTablesGenerator symbolTablesGenerator;
    SemanticAnalyser semanticAnalyser;
    CodeGenerator codeGenerator;

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java main.jmm <file_name> <ignore_exceptions>");
            return;
        }


        jmm compiler = new jmm(args[0]);

        if (args.length == 2)
            compiler.setIgnoreExceptions(Boolean.parseBoolean(args[1]));

        compiler.compileFile();
    }

    public jmm(String pathname) {
        filepath = Paths.get(pathname);
    }

    public void compileFile() throws Exception {
        SimpleNode root;

        // Parsing
        parser = new Parser(this.createInputStream());
        try {
            root = parser.parseProgram(filepath.toString());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new Exception();
        }

        if (!ignoreExceptions)
            root.dump("");

        symbolTablesGenerator = new SymbolTablesGenerator(root);
        symbolTables = symbolTablesGenerator.generate();


        // Semantic analysis
        semanticAnalyser = new SemanticAnalyser(symbolTables, root, ignoreExceptions);
        try {
            semanticAnalyser.startAnalyse();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new Exception();
        }

        codeGenerator = new CodeGenerator(symbolTables);
        codeGenerator.generate();
    }

    private InputStream createInputStream() {
        InputStream input;

        try {
            input = new FileInputStream(filepath.toFile());
        } catch (FileNotFoundException e) {
            System.out.println("File not Found.");
            return null;
        }

        return input;
    }

    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

}
