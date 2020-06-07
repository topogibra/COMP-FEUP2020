package DataFlow;

import main.TypeDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class InterferenceGraph {

    private ArrayList<Node> nodes;
    private Stack<Node> stack;

    public InterferenceGraph(CFG cfg, HashSet<TypeDescriptor> locals) {
        this.nodes = new ArrayList<>();
        this.stack = new Stack<>();

        this.parseNodes(locals);

        int index = 1;
        for (Node node : this.nodes) {

            if (index == locals.size() - 1)
                break;

            for (int i = index; i < locals.size() - 1; i++) {
                if (hasInterference(node.getTypeDescriptor(), this.nodes.get(i).getTypeDescriptor())) {
                    node.addNeighbour(this.nodes.get(i));
                }
            }

            index++;
        }
    }

    public void colorGraph(int k) throws Exception {
        this.simplify(k);

        if (this.stack.size() != this.nodes.size()) {
            throw new Exception();
        }

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            node.colorNode(k);
        }
    }

    private void simplify(int k) {
        for (Node node : this.nodes) {

            if (node.getNumNeighbours() < k) {
                node.setCut(true);

                if (!node.isVisited()) {
                    this.stack.push(node);
                    node.setVisited(true);
                }
            }

            for (Node neighbour : node.getNeighbours()) {
                if (neighbour.isCut())
                    continue;

                if (neighbour.getNumNeighbours() < k) {
                    neighbour.setCut(true);

                    if (!neighbour.isVisited()) {
                        this.stack.push(neighbour);
                        neighbour.setVisited(true);
                    }
                }
            }
        }

    }

    private void parseNodes(HashSet<TypeDescriptor> locals) {
        for (TypeDescriptor local : locals) {
            this.nodes.add(new Node(local));
        }
    }

    public boolean hasInterference(TypeDescriptor var1, TypeDescriptor var2) {
        return Math.max(var1.getLivenessBeginning(), var2.getLivenessBeginning()) < Math.min(var1.getLivenessEnding(), var2.getLivenessEnding());
    }

    public ArrayList<Node> getNodes() {
        return this.nodes;
    }
}
