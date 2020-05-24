
public class Mall {

    private int num_shops;
    private final int[] shops_identifiers;

    public Mall(int num_shops) {
        this.num_shops = num_shops;
        this.shops_identifiers = new int[num_shops];
    }

    public Mall(int[] shops_identifiers) {
        this.shops_identifiers = shops_identifiers;
        this.num_shops = shops_identifiers.length;
    }

    public Mall(int num_shops, int[] shops_identifiers) {
        this.shops_identifiers = new int[num_shops];

        int i;
        i = 0;
        while (i < shops_identifiers.length) {
            this.shops_identifiers[i] = shops_identifiers[i];
            i = i + 1;
        }
    }

    public void print() {
        System.out.println("Num shops: " + this.num_shops);

        int i;
        i = 0;
        while (i < this.shops_identifiers.length) {
            System.out.println("Shop id " + i + ": " + this.shops_identifiers[i]);
            i = i + 1;
        }
    }
}
