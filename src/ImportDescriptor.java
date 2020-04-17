import java.util.ArrayList;
import java.util.List;

public class ImportDescriptor {
    private String className;
    private String methodName;

    private TypeDescriptor returnType;
    private final List<TypeDescriptor> arguments;
    private boolean isStatic = false;

    public ImportDescriptor() {
        this.arguments = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public TypeDescriptor getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeDescriptor returnType) {
        this.returnType = returnType;
    }

    public List<TypeDescriptor> getArguments() {
        return arguments;
    }

    public void addArgument(TypeDescriptor typeDescriptor) {
        this.arguments.add(typeDescriptor);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public String getIdentifier() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(this.className).append(this.methodName);

        for (TypeDescriptor typeDescriptor : this.arguments)
            stringBuilder.append(typeDescriptor.getTypeIdentifier());

        return stringBuilder.toString();
    }

}
