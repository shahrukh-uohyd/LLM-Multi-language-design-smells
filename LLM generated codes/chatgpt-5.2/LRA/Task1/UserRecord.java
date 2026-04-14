public class UserRecord {
    public int value;
}

public class NativeAnalyzer {
    public static native long sumValues(UserRecord[] records);
}
