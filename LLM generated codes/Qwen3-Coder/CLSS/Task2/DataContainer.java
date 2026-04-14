public class DataContainer {
    private float value;
    
    public DataContainer(float value) {
        this.value = value;
    }
    
    public float getValue() {
        return value;
    }
    
    public void setValue(float value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "DataContainer{value=" + value + "}";
    }
}