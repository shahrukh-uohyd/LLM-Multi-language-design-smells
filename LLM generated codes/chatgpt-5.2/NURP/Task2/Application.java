public final class Application {

    static {
        NativeLibraryLoader.loadDataHelper();
    }

    public static void main(String[] args) {
        // Safe to call native code here
    }
}
