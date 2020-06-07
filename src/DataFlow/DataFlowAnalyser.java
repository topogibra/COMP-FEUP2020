package DataFlow;

import main.FunctionDescriptor;
import main.SymbolTables;
import main.TypeDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
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

    public void analyse() throws Exception {
        for (Map.Entry<String, FunctionDescriptor> entry : symbolTables.getMethods().entrySet()) {
            CFG cfg = new CFG(entry.getValue());

            if (this.maxNumRegisters != -1 && this.maxNumRegisters < entry.getValue().getNumLocals()) {
                cfg.calcLiveness();
                HashSet<TypeDescriptor> locals = cfg.calcLiveRanges();

                InterferenceGraph ig = new InterferenceGraph(cfg, locals);

                try {
                    ig.colorGraph(this.maxNumRegisters);
                } catch (Exception e) {
                    throw new Exception("Method " + entry.getValue().getMethodName() + " requires more than " + this.maxNumRegisters + " registers.");
                }

                this.setRegisters(ig.getNodes());
            }

            if (this.optimizationMode) {
                //TODO use CFG created to do optimizations
            }
        }
    }

    public void setRegisters(ArrayList<Node> nodes) {
        for (Node node : nodes) {
            TypeDescriptor var = node.getTypeDescriptor();
            var.setIndex(node.getColor() + 1);
        }
    }
}
