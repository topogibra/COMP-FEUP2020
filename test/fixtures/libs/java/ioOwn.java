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

}
