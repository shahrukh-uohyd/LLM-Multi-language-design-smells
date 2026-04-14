public class Student {
    private String name;
    private int rollNumber;
    private double mathGrade;
    private double scienceGrade;
    private double literatureGrade;

    // Constructor
    public Student(String name, int rollNumber, double mathGrade, double scienceGrade, double literatureGrade) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.mathGrade = mathGrade;
        this.scienceGrade = scienceGrade;
        this.literatureGrade = literatureGrade;
    }

    // Getters
    public String getName() { return name; }
    public int getRollNumber() { return rollNumber; }
    public double getMathGrade() { return mathGrade; }
    public double getScienceGrade() { return scienceGrade; }
    public double getLiteratureGrade() { return literatureGrade; }

    // Setters
    public void setMathGrade(double mathGrade) { this.mathGrade = mathGrade; }
    public void setScienceGrade(double scienceGrade) { this.scienceGrade = scienceGrade; }
    public void setLiteratureGrade(double literatureGrade) { this.literatureGrade = literatureGrade; }

    // Native method to compute average grade
    public native double computeAverageGrade();

    // Alternative native method with custom weights
    public native double computeWeightedAverage(double mathWeight, double scienceWeight, double literatureWeight);

    // Static block to load the native library
    static {
        System.loadLibrary("student_grade"); // loads libstudent_grade.so (Linux/Mac) or student_grade.dll (Windows)
    }
}