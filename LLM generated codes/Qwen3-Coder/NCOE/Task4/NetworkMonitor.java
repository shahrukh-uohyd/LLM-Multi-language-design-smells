// NetworkMonitor.java (Example usage)
import java.util.Random;

public class NetworkMonitor implements Runnable {
    private volatile boolean running = true;
    private Random random = new Random();
    
    public static void main(String[] args) {
        NetworkMonitor monitor = new NetworkMonitor();
        Thread monitorThread = new Thread(monitor, "NetworkMonitor");
        monitorThread.start();
        
        // Let it run for a few seconds
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        monitor.stop();
        
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void stop() {
        running = false;
    }
    
    @Override
    public void run() {
        while (running) {
            // Simulate receiving network packets
            NetworkPacket packet = new NetworkPacket(
                generateRandomIP(), 
                generateRandomIP(), 
                random.nextInt(1500) + 64
            );
            
            System.out.println("Created packet: " + packet);
            
            // Simulate some processing time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            // Refresh the packet (second call site for recordTimestamp)
            long oldTimestamp = packet.getTimestamp();
            packet.refresh();
            System.out.println("Refreshed packet: " + packet + 
                             " (timestamp changed from " + oldTimestamp + " to " + packet.getTimestamp() + ")");
        }
    }
    
    private String generateRandomIP() {
        return random.nextInt(256) + "." + random.nextInt(256) + "." + 
               random.nextInt(256) + "." + random.nextInt(256);
    }
}