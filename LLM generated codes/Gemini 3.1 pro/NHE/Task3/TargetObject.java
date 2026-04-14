public class TargetObject {
    // Several fields of different types and visibilities
    private int id;
    public String name;
    protected double balance;
    boolean isActive;

    public TargetObject(int id, String name, double balance, boolean isActive) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.isActive = isActive;
    }

    // Several methods
    public void activate() {
        this.isActive = true;
    }

    private void resetBalance() {
        this.balance = 0.0;
    }

    public String getInfo() {
        return name + " [" + id + "]";
    }
}