import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayDescriptor extends TypeDescriptor {
    private List<Boolean> array;
    private int arraySize;

    public ArrayDescriptor(String typeIdentifier) {
        super(typeIdentifier);
        this.isArray = true;
    }

    //a = new int[4];
    //a[2] = 4;
    // a[4] = 5;
    //int b;
    //b = a[2];

    public void init(int arraySize) {
        this.init = true;
        this.arraySize = arraySize;
        this.array = new ArrayList<>(Collections.nCopies(arraySize, false));
    }

    public void initVal(int position) {
        this.array.set(position, true);
    }

    public boolean isOutOfBounds(int position) {
        return position < 0 || position >= this.arraySize;
    }

    public boolean posIsInited(int position) {
        return this.array.get(position);
    }

    public int getArraySize() {
        return arraySize;
    }
}
