// UserAccount.java
public class UserAccount {

    private String username;
    private int age;
    private boolean active;

    public UserAccount(String username, int age, boolean active) {
        this.username = username;
        this.age = age;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}
