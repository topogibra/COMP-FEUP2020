public class TypeDescriptor {
    private final String typeIdentifier;
    private boolean isArray = false;
    private boolean init;

    public TypeDescriptor(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
        this.init = false;


       if (this.typeIdentifier.equals("int[]"))
            this.isArray = true;
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
}
