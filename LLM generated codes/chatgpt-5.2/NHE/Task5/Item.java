public class Item {

    private int value;

    public Item(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Native method
    public static native int sumValues(Item[] items);
}
