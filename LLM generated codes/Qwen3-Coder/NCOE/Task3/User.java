// User.java
public class User {
    private int userId;
    private String status;
    private int loginAttempts;
    
    public User(int userId, String status, int loginAttempts) {
        this.userId = userId;
        this.status = status;
        this.loginAttempts = loginAttempts;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public int getLoginAttempts() {
        return loginAttempts;
    }
    
    @Override
    public String toString() {
        return "User{userId=" + userId + ", status='" + status + "', loginAttempts=" + loginAttempts + "}";
    }
}