package main;

import Types.VarTypes;

public class TypeDescriptor {
    private final String typeIdentifier;
    private boolean isArray = false;
    private int index;

    private boolean init_in_if;
    private boolean init_in_else;

    public TypeDescriptor(String typeIdentifier, int index) {
        this.typeIdentifier = typeIdentifier;
        this.index = index;

        this.init_in_if = false;
        this.init_in_else = false;

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
        return init_in_else && init_in_if;
    }

    public void setInit(boolean init) {
        this.init_in_if = init;
        this.init_in_else = init;
    }

    public void setInitInIF(boolean init) {
        this.init_in_if = init;
    }

    public void setInitInElse(boolean init) {
        this.init_in_else = init;
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
