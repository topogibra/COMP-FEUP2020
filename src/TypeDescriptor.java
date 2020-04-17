public class TypeDescriptor {
    private final String typeIdentifier;
    boolean isArray = false;

    public TypeDescriptor(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;

       if (this.typeIdentifier.equals("int[]"))
            this.isArray = true;
    }

    public String getTypeIdentifier() {
        return this.typeIdentifier;
    }

}
