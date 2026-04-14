/**
 * Interface for interpreting low-level data formats
 */
public interface DataFormatInterpreter {
    /**
     * Interpret low-level data format
     */
    InterpretedData interpret(byte[] lowLevelData);
}