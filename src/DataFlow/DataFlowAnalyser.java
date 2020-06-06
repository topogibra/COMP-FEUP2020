package DataFlow;

import main.FunctionDescriptor;
import main.SymbolTables;

import java.util.Map;

public class DataFlowAnalyser {

    private SymbolTables symbolTables;
    private final int maxNumRegisters;
    private final boolean optimizationMode;

    public DataFlowAnalyser(SymbolTables symbolTables, int maxNumRegisters, boolean optimizationMode) {
        this.symbolTables = symbolTables;
        this.maxNumRegisters = maxNumRegisters;
        this.optimizationMode = optimizationMode;
    }

    public void analyse() {
        for (Map.Entry<String, FunctionDescriptor> entry : symbolTables.getMethods().entrySet()) {
            CFG cfg = new CFG(entry.getValue());

            if (this.maxNumRegisters != -1 && this.maxNumRegisters < entry.getValue().getNumLocals()) {
                cfg.calcLiveness();

            }

            if (this.optimizationMode) {


            }
        }
    }
}
