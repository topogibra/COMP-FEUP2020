package DataFlow;

import main.TypeDescriptor;
import java.util.HashSet;

public class Node {
    private TypeDescriptor typeDescriptor;
    private int color = -1;
    private HashSet<Node> neighbours;
    private boolean visited = false;
    private boolean isCut = false;

    public Node(TypeDescriptor typeDescriptor) {
        this.neighbours = new HashSet<>();
        this.typeDescriptor = typeDescriptor;
    }

    public void addNeighbour(Node node) {
        if (this.neighbours.contains(node))
            return;

        this.neighbours.add(node);
        node.addNeighbour(this);
    }

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }

    public boolean isColored() {
        return this.color != -1;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public HashSet<Node> getNeighbours() {
        return this.neighbours;
    }

    public int getColor() {
        return this.color;
    }

    public boolean isCut() {
        return isCut;
    }

    public void setCut(boolean cut) {
        isCut = cut;
    }

    public int getNumNeighbours() {
        int sum = 0;

        for (Node neighbour : this.neighbours) {
            if (!neighbour.isCut())
                sum++;
        }

        return sum;
    }

    public void colorNode(int k) throws Exception {
        HashSet<Integer> colorsUsed = new HashSet<>();

        for (Node neighbour : this.neighbours) {
            if (neighbour.isColored())
                colorsUsed.add(neighbour.getColor());
        }

        for (int i = 0; i < k; i++) {
            if (!colorsUsed.contains(i)) {
                this.color = i;
                break;
            }
        }

        if (this.color == -1)
            throw new Exception();
    }

}
