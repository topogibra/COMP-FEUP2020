package main;

import DataFlow.DataFlowAnalyser;
import parser.Parser;
import parser.SimpleNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class jmm {

    public static Path filepath;
    public static final String DEBUG_FLAG = "-d";
    public static final String REGISTERS_FLAG = "-r";
    public static final String OPTIMIZATION_FLAG = "-o";
    private static final int MAX_NUM_REGISTERS = -1;

    private boolean debugMode = true;
    private boolean optimizationMode = false;
    private int maxNumRegisters = MAX_NUM_REGISTERS;

    private Parser parser;
    private SymbolTables symbolTables;
    private SymbolTablesGenerator symbolTablesGenerator;
    private SemanticAnalyser semanticAnalyser;
    private CodeGenerator codeGenerator;
    private DataFlowAnalyser dataFlowAnalyser;

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 4) {
            System.err.println("Usage: java main.jmm [-d] [-r=<num>] [-o] <input_file.jmm>");
            return;
        }

        jmm compiler = new jmm(args[args.length - 1]);

        for (String s : args) {
            if (s.equals(jmm.DEBUG_FLAG))
                compiler.setDebugMode(false);
            else if (s.equals(jmm.OPTIMIZATION_FLAG))
                compiler.setOptimizationMode(true);
            else if (s.matches(jmm.REGISTERS_FLAG + "=\\d+"))
                compiler.setMaxNumRegisters(Integer.parseInt(s.substring(3)));
        }

        compiler.compileFile();
    }

    public jmm(String pathname) {
        filepath = Paths.get(pathname);
    }

    public void compileFile() throws Exception {
        SimpleNode root;

        // Parsing
        this.parser = new Parser(this.createInputStream());
        try {
            root = this.parser.parseProgram(this.filepath.toString());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new Exception();
        }

        if (this.debugMode)
            root.dump("");

        this.symbolTablesGenerator = new SymbolTablesGenerator(root);
        this.symbolTables = this.symbolTablesGenerator.generate();

        // Semantic analysis
        this.semanticAnalyser = new SemanticAnalyser(this.symbolTables, root, !this.debugMode);
        try {
            this.semanticAnalyser.startAnalyse();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            throw new Exception();
        }

        //R-Option and Optimization
        if (this.maxNumRegisters != MAX_NUM_REGISTERS || this.optimizationMode){
            this.dataFlowAnalyser = new DataFlowAnalyser(this.symbolTables, this.maxNumRegisters, this.optimizationMode);
            this.dataFlowAnalyser.analyse();
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
