public class UserData {
    private int id;
    private String name;
    private double balance;

    // Constructor used by both Java and Native C code
    public UserData(int id, String name, double balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getBalance() { return balance; }

    @Override
    public String toString() {
        return "UserData{id=" + id + ", name='" + name + "', balance=" + balance + "}";
    }
}