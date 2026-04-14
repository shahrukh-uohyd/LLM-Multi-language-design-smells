// src/java/FailureState.java
public enum FailureState {
    HEALTHY(0),
    WARNING(1),
    FAILURE(2),
    CRITICAL(3),
    UNKNOWN(-1);
    
    private final int code;
    
    FailureState(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static FailureState fromCode(int code) {
        for (FailureState state : FailureState.values()) {
            if (state.getCode() == code) {
                return state;
            }
        }
        return UNKNOWN;
    }
}