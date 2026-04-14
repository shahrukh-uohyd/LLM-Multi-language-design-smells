public class JniReflection {
    
    // Load the shared C++ library at runtime
    static {
        System.loadLibrary("jnireflect");
    }

    // Native method: locates a class member, retrieves its reflective information, and returns it
    public native String getMemberInfo(String className, String memberName, String signature, boolean isMethod);

    public static void main(String[] args) {
        JniReflection app = new JniReflection();
        
        System.out.println("Java: Querying Native Code for Class Member Information...");
        System.out.println("-------------------------------------------------");
        
        // 1. Ask JNI to locate a protected field and return its information
        String fieldInfo = app.getMemberInfo("TargetSystem", "systemName", "Ljava/lang/String;", false);
        
        // 2. Ask JNI to locate a private method and return its information
        String methodInfo = app.getMemberInfo("TargetSystem", "initializeSystem", "()V", true);
        
        // Consume the returned information in Java
        System.out.println("Field Info retrieved via JNI: \n  -> " + fieldInfo);
        System.out.println("\nMethod Info retrieved via JNI: \n  -> " + methodInfo);
        System.out.println("-------------------------------------------------");
    }
}