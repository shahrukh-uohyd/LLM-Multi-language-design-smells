public class ExampleUsage {
    public static void main(String[] args) {
        StatusHandler handler = new StatusHandler() {
            @Override
            public void onStatusUpdate(int statusCode, String description) {
                System.out.println("Status: " + statusCode + " - " + description);
            }
        };
        
        long handlerId = NativeComponent.registerHandler(handler);
        NativeComponent nativeComp = new NativeComponent();
        nativeComp.startCalculation(handlerId);
        
        // Keep the main thread alive to see the updates
        try {
            Thread.sleep(10000); // Wait for calculation to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}