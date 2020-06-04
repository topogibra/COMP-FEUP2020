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
            if (((SimpleNode) child).getNodeName().equals(NodeName.METHODBODY)) {
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


            index = this.buildUseAndDef(index, statement,i);


//            this.buildIn(index, statement);
        }
    }

    private void initializeSet(int index) {
        if (this.succ.size() == index) {
            this.succ.add(index, new HashSet<>());
        }
        if (this.pred.size() == index) {
            this.pred.add(index, new HashSet<>());
        }
        if (this.use.size() == index) {
            this.use.add(index, new HashSet<>());
        }
        if (this.def.size() == index) {
            this.def.add(index, new HashSet<>());
        }
        if (this.in.size() == index) {
            this.in.add(index, new HashSet<>());
        }
        if (this.out.size() == index) {
            this.out.add(index, new HashSet<>());
        }
    }

    private int buildUseAndDef(int index, SimpleNode statement, int nodeNumber) {
        switch (statement.getNodeName()) {
            case NodeName.ASSIGNMENT: {
                SimpleNode leftSide = statement.getChild(0);
                SimpleNode rightSide = statement.getChild(1);

                if (leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
                    TypeDescriptor typeDescriptor = this.functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
                    this.def.get(index).add(typeDescriptor);
                }

                this.buildUse(index, rightSide);

                this.setNormalPred(index);
                this.setNormalSuc(index, statement);

                break;
            }
            case NodeName.IF: {
                return this.buildIf(index, statement, (nodeNumber == statement.getParent().jjtGetNumChildren() - 1));
            }
            case NodeName.WHILE: {
                break;
            }
            default: {
                this.buildUse(index, statement);
                this.setNormalPred(index);
                this.setNormalSuc(index, statement);
            }


        }
        return index;
    }

    private int buildIf(int index, SimpleNode statement, boolean lastStatement) { // TODO: Else if, else if
        int lastindex = index;
        boolean hasElse = statement.getChild(2).jjtGetNumChildren() > 0;
        int lastIfBlockStatement = -1;

        // Condition
        SimpleNode cond = statement.getChild(0);

        this.setNormalPred(lastindex);
        this.setNormalSuc(lastindex, cond);
        this.buildUse(lastindex, cond);

        lastindex++;
        this.initializeSet(lastindex);

        int childno = 0;
        //If Block
        for (Node n : statement.getChild(1).jjtGetChildren()) {
            SimpleNode node = (SimpleNode) n;
            this.initializeSet(lastindex);

            this.setNormalPred(lastindex);
            if (childno == (node.getParent().jjtGetNumChildren() - 1)) {
                lastIfBlockStatement = lastindex;
                this.setNormalSuc(lastindex, node);
            } else {
                this.setNormalSuc(lastindex, node);
            }

            this.buildUse(lastindex, node);
            lastindex++;
            childno++;
        }

        childno = 0;
        //Else block
        if (hasElse) {
            for (Node n : statement.getChild(2).jjtGetChildren()) {
                SimpleNode node = (SimpleNode) n;
                this.initializeSet(lastindex);

                if (childno == 0) {
                    this.pred.get(lastindex).add(index);
                } else {
                    this.setNormalPred(lastindex);
                }
                if(childno == (node.getParent().jjtGetNumChildren() - 1) && !lastStatement){
                    this.setNormalSuc(lastindex, node);
                }

                this.buildUse(lastindex, node);
                lastindex++;
                childno++;
            }
        }

        // Fix sucessor of last if block statement
        if (hasElse && !lastStatement) {
            assert (lastIfBlockStatement != -1);
            this.succ.get(lastIfBlockStatement).add(lastindex);
        }

        // Fix statement predecessors after if
        if (!lastStatement) {
            this.initializeSet(lastindex);
            if (hasElse) {
                this.pred.get(lastindex).add(lastIfBlockStatement);
            }
            this.pred.get(lastindex).add(lastindex - 1);
        }
        return lastindex;
    }

    private void setNormalPred(int index) {
        if (index > 0) //Not first statement
            this.pred.get(index).add(index - 1);
    }

    private void setNormalSuc(int index, SimpleNode statement) {
        if (index != (statement.jjtGetParent().jjtGetNumChildren() - 1)) //Not last statement
            this.succ.get(index).add(index + 1);
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
