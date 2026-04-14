public class Main {
    public static void main(String[] args) {

        Student student = new Student(
            "Alice Johnson",   // name          — never reaches native code
            42,                // rollNumber    — never reaches native code
            88.5,              // mathGrade     — passed to native
            91.0,              // scienceGrade  — passed to native
            76.5               // literatureGrade — passed to native
        );

        // APPROACH B — recommended
        double average = student.computeAverageGradeSecure();
        // (88.5 + 91.0 + 76.5) / 3 = 256.0 / 3 = 85.33...

        System.out.printf("Student     : %s%n", student.getName());
        System.out.printf("Roll Number : %d%n", student.getRollNumber());
        System.out.printf("Math        : %.1f%n", student.getMathGrade());
        System.out.printf("Science     : %.1f%n", student.getScienceGrade());
        System.out.printf("Literature  : %.1f%n", student.getLiteratureGrade());
        System.out.printf("Average     : %.2f%n", average);
        System.out.printf("Grade Band  : %s%n",   gradeBand(average));
    }

    private static String gradeBand(double avg) {
        if (avg  < 0)   return "INVALID INPUT";
        if (avg >= 90)  return "A  — Excellent";
        if (avg >= 75)  return "B  — Good";
        if (avg >= 60)  return "C  — Average";
        if (avg >= 45)  return "D  — Below Average";
        return                 "F  — Fail";
    }
}