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

    private boolean debugMode = true;
    private boolean optimizationMode = false;
    private int maxNumRegisters = 0;

    private Parser parser;
    private SymbolTables symbolTables;
    private SymbolTablesGenerator symbolTablesGenerator;
    private SemanticAnalyser semanticAnalyser;
    private CodeGenerator codeGenerator;

    public static final String DEBUG_FLAG = "-d";
    public static final String REGISTERS_FLAG = "-r";
    public static final String OPTIMIZATION_FLAG = "-o";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 4) {
            System.err.println("Usage: java main.jmm [-d] [-r=<num>] [-o] <input_file.jmm>");
            return;
        }

        jmm compiler = new jmm(args[0]);

        for (int i = 0; i < args.length - 1; i++) {
            String s = args[i];
            if (s.equals(jmm.DEBUG_FLAG))
                compiler.setDebugMode(false);
            else if (s.equals(jmm.OPTIMIZATION_FLAG))
                compiler.setOptimizationMode(true);
            else if (s.matches(jmm.REGISTERS_FLAG + "=\\d+"))
                compiler.setMaxNumRegisters(Integer.parseInt(s.substring(2)));
        }

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

        if (debugMode)
            root.dump("");

        symbolTablesGenerator = new SymbolTablesGenerator(root);
        symbolTables = symbolTablesGenerator.generate();

        // Semantic analysis
        semanticAnalyser = new SemanticAnalyser(symbolTables, root, !debugMode);
        try {
            semanticAnalyser.startAnalyse();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new Exception();
        }

        codeGenerator = new CodeGenerator(symbolTables, optimizationMode, maxNumRegisters);
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

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setOptimizationMode(boolean optimizationMode) {
        this.optimizationMode = optimizationMode;
    }

    public void setMaxNumRegisters(int maxNumRegisters) {
        this.maxNumRegisters = maxNumRegisters;
    }
}
