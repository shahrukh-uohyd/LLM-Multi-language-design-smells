package com.app.device;

/**
 * Handles device control operations and exposes native methods
 * for communicating with hardware peripherals.
 *
 * <p>This class is NOT significantly modified — shown as architectural
 * context only. The native library it loads ({@code device_controller})
 * is fully independent from the new signal processing library.</p>
 */
public class DeviceController {

    static {
        System.loadLibrary("device_controller"); // existing — unchanged
    }

    // ── existing native declarations (all unchanged) ──────────────────

    /** Opens a hardware device channel by the given device ID. */
    public native int openDevice(String deviceId);

    /** Closes a previously opened device channel. */
    public native void closeDevice(int deviceHandle);

    /** Sends a raw command byte buffer to the device. */
    public native int sendCommand(int deviceHandle, byte[] command);

    /** Reads a raw data buffer from the device into the provided array. */
    public native int readData(int deviceHandle, byte[] buffer, int maxLength);

    /** Returns the current status register value of the device. */
    public native int getDeviceStatus(int deviceHandle);

    /** Resets the device to its default hardware state. */
    public native void resetDevice(int deviceHandle);
}