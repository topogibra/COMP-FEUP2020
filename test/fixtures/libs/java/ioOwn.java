import java.util.Scanner;

public class ioOwn {

    public static void printWinner() {
        System.out.println("Winner!!");
    }

    public static void printLoser() {
        System.out.println("Loser!!");
    }

    private static int requestNumber(String request) {
        System.out.print(request);
        Scanner var0 = new Scanner(System.in);
        int var1 = var0.nextInt();
        return var1;
    }

    public static int requestArraySize() {
        return requestNumber("Insert array size: ");
    }

    public static int requestArrayIndex() {
        return requestNumber("Insert array index: ");
    }

    public static int requestHeight() {
        return requestNumber("Insert your height (cm): ");
    }

    public static int requestWeight() {
        return requestNumber("Insert your weight (Kg): ");
    }

    public static void printExceededSize() {
        System.out.println("Array size exceeded");
    }

    public static void reachedMaxTries(int maxTries) {
        System.out.println("Reached max number of tries (max: " + maxTries + ")");
    }

    public static void printCalcMenu() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("###### MENU ######\n");
        stringBuilder.append("1 - Addition\n");
        stringBuilder.append("2 - Subtraction\n");
        stringBuilder.append("3 - Multiplication\n");
        stringBuilder.append("4 - Division\n");
        stringBuilder.append("0 - Exit\n\n");

        System.out.println(stringBuilder.toString());
    }

    public static void printInvalidOperation() {
        System.out.println("Invalid Operation");
    }

    public static void printIMC(int label) {
        System.out.println(getIMCDegree(label));
    }

    public static String getIMCDegree(int label) {
        switch (label) {
            case 0: return "Magreza grave";
            case 1: return "Magreza moderada";
            case 2: return "Magreza leve";
            case 3: return "Saudável";
            case 4: return "Sobrepeso";
            case 5: return "Obesidade grau I";
            case 6: return "Obesidade grau II (severa)";
            case 7: return "Obesidade grau III (mórbida)";
        }

        return null;
    }


}
