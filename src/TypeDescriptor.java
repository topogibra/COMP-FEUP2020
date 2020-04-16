public class TypeDescriptor {
    String typeIdentifier;
    boolean isArray = false;

    public TypeDescriptor(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;

       if (this.typeIdentifier.equals("int[]"))
            this.isArray = true;
    }

}
