import java.util.Scanner;

public class ioOwn {

    public static void printWinner() {
        System.out.println("Winner!!");
    }

    public static void printLoser() {
        System.out.println("Loser!!");
    }

    public static int requestArraySize() {
        System.out.print("Insert array size: ");
        Scanner var0 = new Scanner(System.in);
        int var1 = var0.nextInt();
        return var1;
    }

    public static int requestArrayIndex() {
        System.out.print("Insert array index: ");
        Scanner var0 = new Scanner(System.in);
        int var1 = var0.nextInt();
        return var1;
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


}
