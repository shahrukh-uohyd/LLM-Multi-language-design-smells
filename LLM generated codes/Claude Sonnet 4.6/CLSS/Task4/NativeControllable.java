package com.example.app;

/**
 * Marker interface for all Java components that can be
 * located and triggered by the native C++ module via JNI.
 */
public interface NativeControllable {

    /**
     * Returns a unique string identifier for this component.
     * Used by the native layer to locate a specific component.
     */
    String getComponentId();

    /**
     * Called by the native module to trigger this component's
     * primary functionality.
     *
     * @param params  A descriptive parameter string from native code.
     * @param flags   Bitmask flags that alter behaviour.
     * @return        A result/status string reported back to native.
     */
    String trigger(String params, int flags);
}