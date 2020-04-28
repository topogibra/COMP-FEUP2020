import Types.VarTypes;

public class TypeDescriptor {
    private final String typeIdentifier;
    private boolean isArray = false;
    private boolean init;
    private int index;

    public TypeDescriptor(String typeIdentifier, int index) {
        this.typeIdentifier = typeIdentifier;
        this.init = false;
        this.index = index;

       if (this.typeIdentifier.equals("int[]"))
            this.isArray = true;
    }

    public TypeDescriptor(String typeIdentifier) {
        this(typeIdentifier, -1);
    }

    public String getTypeIdentifier() {
        return this.typeIdentifier;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    public String toJVM() {
        return toJVM(this.typeIdentifier);
    }

    public static String toJVM(String typeIdentifier) {
        switch (typeIdentifier) {
            case VarTypes.INT:
            case VarTypes.BOOLEAN:
                return "I";
            case VarTypes.INTARRAY: return "[I";
            case VarTypes.VOID: return "V";
            default: return typeIdentifier;
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
