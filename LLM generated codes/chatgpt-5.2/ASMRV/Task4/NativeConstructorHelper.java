// File: NativeConstructorHelper.java
public class NativeConstructorHelper {

    static {
        System.loadLibrary("constructor_helper");
    }

    // Native method prepares data for UserAccount construction
    public native ConstructorData prepareConstructorData(int seed);

    public static void main(String[] args) {
        NativeConstructorHelper helper = new NativeConstructorHelper();

        // Native code prepares constructor arguments
        ConstructorData data = helper.prepareConstructorData(7);

        // Java uses the returned data to construct the object
        UserAccount account = new UserAccount(data.id, data.username);

        System.out.println(account);
    }
}
