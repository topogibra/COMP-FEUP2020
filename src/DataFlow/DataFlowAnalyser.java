package DataFlow;

import main.FunctionDescriptor;
import main.SymbolTables;

import java.util.Map;

public class DataFlowAnalyser {

    private SymbolTables symbolTables;

    public DataFlowAnalyser(SymbolTables symbolTables) {
        this.symbolTables = symbolTables;
    }

    public void analyse() {
        for (Map.Entry<String, FunctionDescriptor> entry : symbolTables.getMethods().entrySet()) {
            CFG cfg = new CFG(entry.getValue());
        }
    }
}
