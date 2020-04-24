public class TypeDescriptor {
    private final String typeIdentifier;
    private boolean isArray = false;
    public boolean init;

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
}
