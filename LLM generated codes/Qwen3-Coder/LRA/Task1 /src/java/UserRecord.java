// src/java/UserRecord.java
public class UserRecord {
    private int id;
    private String name;
    private int score;
    private int age;
    
    public UserRecord(int id, String name, int score, int age) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.age = age;
    }
    
    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public int getAge() { return age; }
    
    @Override
    public String toString() {
        return "UserRecord{id=" + id + ", name='" + name + "', score=" + score + ", age=" + age + "}";
    }
}