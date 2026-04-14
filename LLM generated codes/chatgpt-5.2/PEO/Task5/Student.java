public class Student {

    private String name;
    private int rollNumber;

    // Grades
    private double mathGrade;
    private double scienceGrade;
    private double literatureGrade;

    static {
        System.loadLibrary("studentjni");
    }

    public Student(String name,
                   int rollNumber,
                   double mathGrade,
                   double scienceGrade,
                   double literatureGrade) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.mathGrade = mathGrade;
        this.scienceGrade = scienceGrade;
        this.literatureGrade = literatureGrade;
    }

    // Native method to compute average grade
    public native double computeAverageGrade();

    // Demo
    public static void main(String[] args) {
        Student s = new Student("Alice", 101, 85.0, 90.0, 95.0);
        System.out.println("Average grade: " + s.computeAverageGrade());
    }
}
