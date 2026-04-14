public class Main {

    public static void main(String[] args) {

        String original = "JNI serialization example 🚀";

        byte[] serialized = NativeSerializer.serialize(original);
        String restored = NativeSerializer.deserialize(serialized);

        System.out.println("Original:   " + original);
        System.out.println("Restored:   " + restored);
        System.out.println("Equal:      " + original.equals(restored));
    }
}
