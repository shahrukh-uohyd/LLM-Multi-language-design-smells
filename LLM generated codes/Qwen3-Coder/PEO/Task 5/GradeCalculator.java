public class GradeCalculator {
    
    /**
     * Computes the simple average of the student's grades
     * @param student The student object
     * @return Average grade (arithmetic mean of math, science, and literature grades)
     */
    public static double computeSimpleAverage(Student student) {
        return student.computeAverageGrade();
    }
    
    /**
     * Computes the weighted average of the student's grades
     * @param student The student object
     * @param mathWeight Weight for math grade
     * @param scienceWeight Weight for science grade  
     * @param literatureWeight Weight for literature grade
     * @return Weighted average grade
     */
    public static double computeWeightedAverage(Student student, 
                                               double mathWeight, 
                                               double scienceWeight, 
                                               double literatureWeight) {
        return student.computeWeightedAverage(mathWeight, scienceWeight, literatureWeight);
    }
    
    /**
     * Determines letter grade based on numeric average
     * @param averageGrade The computed average grade
     * @return Letter grade (A, B, C, D, F)
     */
    public static String getLetterGrade(double averageGrade) {
        if (averageGrade >= 90.0) return "A";
        else if (averageGrade >= 80.0) return "B";
        else if (averageGrade >= 70.0) return "C";
        else if (averageGrade >= 60.0) return "D";
        else return "F";
    }
    
    /**
     * Checks if student passed (average >= 60)
     * @param averageGrade The computed average grade
     * @return true if passed, false otherwise
     */
    public static boolean hasPassed(double averageGrade) {
        return averageGrade >= 60.0;
    }
}