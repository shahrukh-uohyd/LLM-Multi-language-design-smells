// File: UserAccount.java
public class UserAccount {
    private int id;
    private String username;

    public UserAccount(int id) {
        this(id, "unknown");
    }

    public UserAccount(int id, String username) {
        this.id = id;
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserAccount{id=" + id + ", username='" + username + "'}";
    }
}
