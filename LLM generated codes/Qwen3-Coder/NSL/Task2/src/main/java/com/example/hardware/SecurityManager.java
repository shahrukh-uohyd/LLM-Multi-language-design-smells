// File: src/main/java/com/example/hardware/SecurityManager.java
package com.example.hardware;

import java.security.Permission;

/**
 * Custom security manager for hardware information access
 * This class enforces security policies for native library loading
 */
public class SecurityManager extends java.lang.SecurityManager {
    private static final String NATIVE_LIBRARY_PERMISSION = "loadLibrary.hardwareinfo";
    
    @Override
    public void checkPermission(Permission perm) {
        String name = perm.getName();
        
        // Restrict native library loading to approved libraries
        if (name != null && name.startsWith("loadLibrary.")) {
            if (!NATIVE_LIBRARY_PERMISSION.equals(name)) {
                throw new SecurityException("Unauthorized attempt to load native library: " + name);
            }
        }
        
        // Allow other permissions normally
        super.checkPermission(perm);
    }
    
    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }
}