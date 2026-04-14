import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SensorDecoder {

    // Load the native shared library
    static {
        System.loadLibrary("sensordecoder");
    }

    /**
     * A structured class representing a decoded sensor reading.
     */
    public static class SensorReading {
        public int sensorId;
        public int timestamp;
        public float value;

        // Constructor invoked by the C native code
        public SensorReading(int sensorId, int timestamp, float value) {
            this.sensorId = sensorId;
            this.timestamp = timestamp;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("SensorID: %03d | Timestamp: %10d | Value: %.2f", 
                                 sensorId, timestamp, value);
        }
    }

    /**
     * Native method that receives a raw binary sensor stream (primitive byte array),
     * decodes the protocol, and returns an array of structured SensorReading objects.
     * 
     * Protocol definition (9 bytes per reading, Little-Endian):
     * [0]    : Sensor ID (1 byte, unsigned)
     * [1..4] : Timestamp (4 bytes, 32-bit integer)
     * [5..8] : Value (4 bytes, 32-bit float)
     * 
     * @param stream The primitive array representing the raw data stream.
     * @return An array of decoded SensorReading objects.
     */
    public native SensorReading[] decodeStream(byte[] stream);

    public static void main(String[] args) {
        SensorDecoder decoder = new SensorDecoder();

        // 1. Simulate an incoming sensor data stream (represented as a primitive byte array)
        // We will generate 3 packets (3 * 9 bytes = 27 bytes total)
        ByteBuffer buffer = ByteBuffer.allocate(27);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Simulate a little-endian hardware sensor

        // Packet 1: Sensor 12, Time 1690000000, Value 25.5
        buffer.put((byte) 12).putInt(1690000000).putFloat(25.5f);
        // Packet 2: Sensor 45, Time 1690000005, Value -14.2
        buffer.put((byte) 45).putInt(1690000005).putFloat(-14.2f);
        // Packet 3: Sensor 99, Time 1690000010, Value 100.0
        buffer.put((byte) 99).putInt(1690000010).putFloat(100.0f);

        byte[] rawStream = buffer.array();

        System.out.println("--- JNI Sensor Data Decoder ---");
        System.out.println("Received raw stream of " + rawStream.length + " bytes.");
        
        // 2. Pass the data stream to the native method
        SensorReading[] readings = decoder.decodeStream(rawStream);

        // 3. The decoded result is returned to Java
        System.out.println("\nDecoded Readings:");
        if (readings != null) {
            for (SensorReading reading : readings) {
                System.out.println("  " + reading.toString());
            }
        } else {
            System.err.println("Failed to decode the sensor stream.");
        }
    }
}