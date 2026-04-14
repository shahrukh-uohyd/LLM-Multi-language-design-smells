// CompressionService.java
import java.util.ArrayList;
import java.util.List;

public class CompressionService {
    private PlatformUtils platformUtils;
    private List<CompressionObserver> observers;

    public CompressionService() {
        this.platformUtils = new PlatformUtils();
        this.observers = new ArrayList<>();
    }

    public void addObserver(CompressionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(CompressionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String event, byte[] data) {
        for (CompressionObserver observer : observers) {
            observer.onCompressionEvent(event, data);
        }
    }

    /**
     * Compresses large data chunks efficiently
     */
    public byte[] compressLargeData(byte[] data, String algorithm) {
        try {
            notifyObservers("START_COMPRESSION", data);
            byte[] result = platformUtils.compressData(data, algorithm);
            notifyObservers("END_COMPRESSION", result);
            return result;
        } catch (Exception e) {
            System.err.println("Large data compression failed: " + e.getMessage());
            notifyObservers("ERROR_COMPRESSION", data);
            return null;
        }
    }

    /**
     * Decompresses large data chunks efficiently
     */
    public byte[] decompressLargeData(byte[] compressedData, String algorithm) {
        try {
            notifyObservers("START_DECOMPRESSION", compressedData);
            byte[] result = platformUtils.decompressData(compressedData, algorithm);
            notifyObservers("END_DECOMPRESSION", result);
            return result;
        } catch (Exception e) {
            System.err.println("Large data decompression failed: " + e.getMessage());
            notifyObservers("ERROR_DECOMPRESSION", compressedData);
            return null;
        }
    }

    /**
     * Batch compression of multiple data items
     */
    public List<byte[]> batchCompress(List<byte[]> dataList, String algorithm) {
        List<byte[]> results = new ArrayList<>();
        for (byte[] data : dataList) {
            try {
                byte[] compressed = platformUtils.compressData(data, algorithm);
                results.add(compressed);
            } catch (Exception e) {
                System.err.println("Batch compression failed for item: " + e.getMessage());
                results.add(new byte[0]); // Add empty array for failed items
            }
        }
        return results;
    }

    /**
     * Gets compression statistics
     */
    public CompressionStats getCompressionStats(byte[] originalData, byte[] compressedData) {
        int originalSize = originalData.length;
        int compressedSize = compressedData.length;
        double ratio = (double) compressedSize / originalSize;
        double savings = ((double)(originalSize - compressedSize) / originalSize) * 100;
        
        return new CompressionStats(originalSize, compressedSize, ratio, savings);
    }

    /**
     * Multi-algorithm compression test
     */
    public MultiAlgorithmResult testMultiAlgorithm(byte[] data) {
        String[] algorithms = {"GZIP", "DEFLATE"};
        MultiAlgorithmResult result = new MultiAlgorithmResult();
        
        for (String algorithm : algorithms) {
            try {
                byte[] compressed = platformUtils.compressData(data, algorithm);
                CompressionStats stats = getCompressionStats(data, compressed);
                
                if (algorithm.equals("GZIP")) {
                    result.gzipResult = compressed;
                    result.gzipStats = stats;
                } else if (algorithm.equals("DEFLATE")) {
                    result.deflateResult = compressed;
                    result.deflateStats = stats;
                }
            } catch (Exception e) {
                System.err.println("Algorithm " + algorithm + " failed: " + e.getMessage());
            }
        }
        
        return result;
    }

    public static class CompressionStats {
        public final int originalSize;
        public final int compressedSize;
        public final double ratio;
        public final double savingsPercent;

        public CompressionStats(int originalSize, int compressedSize, double ratio, double savingsPercent) {
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.ratio = ratio;
            this.savingsPercent = savingsPercent;
        }
    }

    public static class MultiAlgorithmResult {
        public byte[] gzipResult;
        public byte[] deflateResult;
        public CompressionStats gzipStats;
        public CompressionStats deflateStats;
    }
}