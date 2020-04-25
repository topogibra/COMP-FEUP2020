public class TypeDescriptor {
    private final String typeIdentifier;
    protected boolean isArray = false;
    protected boolean init;

    public TypeDescriptor(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
        this.init = false;
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
}
