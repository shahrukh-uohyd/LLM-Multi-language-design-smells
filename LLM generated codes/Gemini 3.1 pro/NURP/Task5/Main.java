public class Main {
    public static void main(String[] args) {
        // 1. Load the native library
        NativeSerializer.loadLibrary();
        NativeSerializer serializer = new NativeSerializer();

        // 2. Create the initial data object
        UserData originalUser = new UserData(1042, "Alice Turing", 9500.75);
        System.out.println("\n[1] Original Java Object: " + originalUser);

        // 3. Serialize using Native C Code
        byte[] serializedBytes = serializer.serialize(originalUser);
        
        System.out.print("[2] Serialized to Bytes (Native C): ");
        for (byte b : serializedBytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println(" (Total " + serializedBytes.length + " bytes)");

        // 4. Deserialize using Native C Code
        UserData reconstructedUser = serializer.deserialize(serializedBytes);
        System.out.println("[3] Deserialized Java Object: " + reconstructedUser);

        // 5. Verify
        boolean isMatch = originalUser.getId() == reconstructedUser.getId() &&
                          originalUser.getBalance() == reconstructedUser.getBalance() &&
                          originalUser.getName().equals(reconstructedUser.getName());
                          
        System.out.println("\nData integrity verified: " + (isMatch ? "SUCCESS \u2713" : "FAILED \u2717"));
    }
}