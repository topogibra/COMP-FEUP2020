package DataFlow;

import Types.NodeName;
import main.FunctionDescriptor;
import main.TypeDescriptor;
import parser.Node;
import parser.SimpleNode;

import java.util.ArrayList;
import java.util.HashSet;

public class CFG {

    private FunctionDescriptor functionDescriptor;

    //Sets
    private ArrayList<HashSet<Integer>> succ;
    private ArrayList<HashSet<Integer>> pred;

    private ArrayList<HashSet<TypeDescriptor>> use;
    private ArrayList<HashSet<TypeDescriptor>> def;

    private ArrayList<HashSet<TypeDescriptor>> in;
    private ArrayList<HashSet<TypeDescriptor>> out;

    public CFG(FunctionDescriptor functionDescriptor) {
        this.functionDescriptor = functionDescriptor;

        SimpleNode methodBodyNode = this.getMethodBodyNode(functionDescriptor.getMethodNode());
        this.build(methodBodyNode);
    }

    private SimpleNode getMethodBodyNode(SimpleNode methodNode) {
        for (Node child : methodNode.jjtGetChildren()) {
            if ( ((SimpleNode) child).getNodeName().equals(NodeName.METHODBODY) ) {
                return ((SimpleNode) child);
            }
        }

        return null;
    }

    public void build(SimpleNode methodBodyNode) {
        this.succ = new ArrayList<>();
        this.pred = new ArrayList<>();

        this.use = new ArrayList<>();
        this.def = new ArrayList<>();

        this.in = new ArrayList<>();
        this.out = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < methodBodyNode.jjtGetNumChildren(); i++) {

            SimpleNode statement = methodBodyNode.getChild(i);
            if (statement.getNodeName().equals(NodeName.VARDECLARATION))
                continue;

            this.initializeSet(index);

            if (index > 0) //Not first statement
                this.pred.get(index).add(index - 1);

            if (i != (methodBodyNode.jjtGetNumChildren() - 1)) //Not last statement
                this.succ.get(index).add(index + 1);

            this.buildUseAndDef(index, statement);

            this.buildIn(index, statement);

            index++;
        }
    }

    private void initializeSet(int index) {
        this.succ.set(index, new HashSet<>());
        this.pred.set(index, new HashSet<>());

        this.use.set(index, new HashSet<>());
        this.def.set(index, new HashSet<>());

        this.in.set(index, new HashSet<>());
        this.out.set(index, new HashSet<>());
    }

    private void buildUseAndDef(int index, SimpleNode statement) {
        switch (statement.getNodeName()) {
            case NodeName.ASSIGNMENT: {
                SimpleNode leftSide = statement.getChild(0);
                SimpleNode rightSide = statement.getChild(1);

                if (leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
                    TypeDescriptor typeDescriptor = this.functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
                    this.def.get(index).add(typeDescriptor);
                }

                this.buildUse(index, rightSide);

                break;
            }
            case NodeName.IF: {
                this.buildIf(index, statement);
                break;
            }
            case NodeName.WHILE: {
                break;
            }
            default: {
                this.buildUse(index, statement);
            }


        }
    }

    private void buildIf(int index, SimpleNode statement) {


    }

    private void buildUse(int index, SimpleNode statement) {
        if (statement.jjtGetNumChildren() == 0) {
            if (statement.getNodeName().equals(NodeName.IDENTIFIER)) {
                TypeDescriptor typeDescriptor = this.functionDescriptor.getTypeDescriptor(statement.jjtGetVal());
                this.use.get(index).add(typeDescriptor);
            }

            return;
        }

        for (Node node : statement.jjtGetChildren()) {
            SimpleNode child = (SimpleNode) node;

            this.buildUse(index, child);
        }
    }

    private void buildIn(int index, SimpleNode statement) {


    }

    private void buildOut(int index, SimpleNode statement) {

    }
}
