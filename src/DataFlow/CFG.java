package DataFlow;

import Types.NodeName;
import main.FunctionDescriptor;
import main.TypeDescriptor;
import parser.Node;
import parser.SimpleNode;

import java.util.ArrayList;
import java.util.HashSet;

public class CFG {

    private final FunctionDescriptor functionDescriptor;
    private SimpleNode methodBodyNode;

    //Sets
    private ArrayList<HashSet<Integer>> succ;
    private ArrayList<HashSet<Integer>> pred;

    private ArrayList<HashSet<TypeDescriptor>> use;
    private ArrayList<HashSet<TypeDescriptor>> def;

    private ArrayList<HashSet<TypeDescriptor>> in;
    private ArrayList<HashSet<TypeDescriptor>> out;

    public CFG(FunctionDescriptor functionDescriptor) {
        this.functionDescriptor = functionDescriptor;

        try {
            this.methodBodyNode = this.getMethodBodyNode(functionDescriptor.getMethodNode());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        this.build();
    }

    public void build() {
        this.succ = new ArrayList<>();
        this.pred = new ArrayList<>();

        this.use = new ArrayList<>();
        this.def = new ArrayList<>();

        this.in = new ArrayList<>();
        this.out = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < this.methodBodyNode.jjtGetNumChildren(); i++) {

            SimpleNode statement = this.methodBodyNode.getChild(i);
            if (statement.getNodeName().equals(NodeName.VARDECLARATION)) //Ignoring Var declarations
                continue;

            index = this.buildStatement(index, statement) + 1;
        }
    }


    private int buildStatement(int index, SimpleNode statement) {
        this.initializeSet(index);

        switch (statement.getNodeName()) {
            case NodeName.ASSIGNMENT: {
                this.buildAssignment(index, statement);
                this.setNormalPred(index, statement);
                this.setNormalSuc(index, statement);
                break;
            }
            case NodeName.IF: {
                return this.buildIf(index, statement, new HashSet<>());
            }
            case NodeName.WHILE: {
                break;
            }
            default: {
                this.buildUse(index, statement);
                this.setNormalPred(index, statement);
                this.setNormalSuc(index, statement);
            }
        }

        return index;
    }

    private void buildAssignment(int index, SimpleNode assignmentNode) {
        SimpleNode leftSide = assignmentNode.getChild(0);
        SimpleNode rightSide = assignmentNode.getChild(1);

        if (leftSide.getNodeName().equals(NodeName.IDENTIFIER)) {
            TypeDescriptor typeDescriptor = this.functionDescriptor.getTypeDescriptor(leftSide.jjtGetVal());
            this.def.get(index).add(typeDescriptor);
        }

        this.buildUse(index, rightSide);
    }

    private int buildIf(int index, SimpleNode ifStatement, HashSet<Integer> lastBlockStatements) {
        SimpleNode conditionNode = ifStatement.getChild(0);
        SimpleNode ifBlockNode = ifStatement.getChild(1);
        SimpleNode elseNode = ifStatement.getChild(2);
        boolean hasElse = elseNode.jjtGetNumChildren() > 0;

        int lastIfBlockStatement = -1;
        int lastElseBlockStatement = -1;

        // Condition Statement
        int conditionIndex = index;
        this.buildUse(conditionIndex, conditionNode);

        //If Block
        int childNo = 0;
        for (Node n : ifBlockNode.jjtGetChildren()) {
            SimpleNode node = (SimpleNode) n;

            index++;
            this.initializeSet(index);

            this.pred.get(index).add(index - 1);
            this.succ.get(index - 1).add(index);

            if (childNo == (ifBlockNode.jjtGetNumChildren() - 1) && !node.getNodeName().equals(NodeName.IF)) {
                lastIfBlockStatement = index;
            }

            switch (node.getNodeName()) {
                case NodeName.ASSIGNMENT: {
                    this.buildAssignment(index, node);
                    break;
                }
                case NodeName.IF: {
                    index = this.buildIf(index, node, lastBlockStatements);
                    break;
                }
                case NodeName.WHILE: {
                    break;
                }
                default: {
                    this.buildUse(index, node);
                    break;
                }
            }

            childNo++;
        }

        //Else block
        childNo = 0;
        if (hasElse) {
            for (Node n : elseNode.jjtGetChildren()) {
                SimpleNode node = (SimpleNode) n;

                index++;
                this.initializeSet(index);

                if (childNo == 0) {
                    this.pred.get(index).add(conditionIndex);
                    this.succ.get(conditionIndex).add(index);
                }
                else {
                    this.pred.get(index).add(index - 1);
                    this.succ.get(index - 1).add(index);
                }

                if (childNo == (elseNode.jjtGetNumChildren() - 1) && !node.getNodeName().equals(NodeName.IF)) {
                    lastElseBlockStatement = index;
                }

                switch (node.getNodeName()) {
                    case NodeName.ASSIGNMENT: {
                        this.buildAssignment(index, node);
                        break;
                    }
                    case NodeName.IF: {
                        index = this.buildIf(index, node, lastBlockStatements);
                        break;
                    }
                    case NodeName.WHILE: {
                        break;
                    }
                    default: {
                        this.buildUse(index, node);
                        break;
                    }
                }

                childNo++;
            }
        }

        if (( ifStatement.getParent() == this.methodBodyNode && !this.isLastStatement(ifStatement) ) || !this.isLastChild(ifStatement)) {
            index++;
            this.initializeSet(index);

            for (Integer i : lastBlockStatements) {
                this.succ.get(i).add(index);
                this.pred.get(index).add(i);
            }

            lastBlockStatements.clear();

            if (lastIfBlockStatement != -1) {
                this.succ.get(lastIfBlockStatement).add(index);
                this.pred.get(index).add(lastIfBlockStatement);
            }

            if (lastElseBlockStatement != -1) {
                this.succ.get(lastElseBlockStatement).add(index);
                this.pred.get(index).add(lastElseBlockStatement);
            }

            index--;
        } else {
            if (lastIfBlockStatement != -1) {
                lastBlockStatements.add(lastIfBlockStatement);
            }

            if (lastElseBlockStatement != -1) {
                lastBlockStatements.add(lastElseBlockStatement);
            }
        }

        return index;
    }

    private boolean isLastChild(SimpleNode node) {
        return node == node.getParent().getChild(node.getParent().jjtGetNumChildren() - 1);
    }

    private void setNormalPred(int index, SimpleNode statementNode) {
        if (!isFirstStatement(statementNode)) {
            this.pred.get(index).add(index - 1);
            this.succ.get(index - 1).add(index);
        }
    }

    private void setPred(int index, int predIndex, SimpleNode statementNode) {
        if (!this.isFirstStatement(statementNode))
            this.pred.get(index).add(predIndex);
    }

    private void setNormalSuc(int index, SimpleNode statementNode) {
        if (!this.isLastStatement(statementNode)) {//Not last statement
            this.succ.get(index).add(index + 1);
            this.initializeSet(index + 1);
            this.pred.get(index + 1).add(index);
        }
    }

    private void setSuc(int index, int succIndex, SimpleNode statementNode) {
        if (!this.isLastStatement(statementNode)) //Not last statement
            this.succ.get(index).add(succIndex);
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

    private boolean isLastStatement(SimpleNode statementNode) {
        return statementNode == this.methodBodyNode.getChild(this.methodBodyNode.jjtGetNumChildren() - 1);
    }

    private boolean isFirstStatement(SimpleNode statementNode) {
        return statementNode == this.getFirstStatement();
    }

    private SimpleNode getFirstStatement() {
        for (Node n : this.methodBodyNode.jjtGetChildren()) {
            SimpleNode statement = (SimpleNode) n;

            if (!statement.getNodeName().equals(NodeName.VARDECLARATION)) //Ignoring Var declarations
                return statement;
        }

        return null;
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

    private SimpleNode getMethodBodyNode(SimpleNode methodNode) throws Exception {
        for (Node child : methodNode.jjtGetChildren()) {
            if (((SimpleNode) child).getNodeName().equals(NodeName.METHODBODY)) {
                return ((SimpleNode) child);
            }
        }

        throw new Exception("No method body node found");
    }
}
