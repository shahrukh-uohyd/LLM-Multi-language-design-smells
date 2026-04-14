public class DataContainer {

    private double value;

    public DataContainer(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "DataContainer{value=" + value + "}";
    }
}
