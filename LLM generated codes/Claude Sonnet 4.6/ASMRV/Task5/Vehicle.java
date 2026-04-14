/**
 * A class with a variety of fields and methods of different
 * access modifiers and types.
 *
 * The native layer will locate its members indirectly (reflection-like)
 * and return descriptive information about them back to Java.
 */
public class Vehicle {

    // ── Fields of different types and modifiers ────────────────────��─
    public    String  brand;
    public    int     year;
    protected double  engineCapacity;
    private   boolean isElectric;
    public    int     speed;

    // ── Constructors ─────────────────────────────────────────────────
    public Vehicle() {
        this.brand          = "Unknown";
        this.year           = 2000;
        this.engineCapacity = 1.0;
        this.isElectric     = false;
        this.speed          = 0;
    }

    public Vehicle(String brand, int year, double engineCapacity,
                   boolean isElectric, int speed) {
        this.brand          = brand;
        this.year           = year;
        this.engineCapacity = engineCapacity;
        this.isElectric     = isElectric;
        this.speed          = speed;
    }

    // ── Methods of different return types and modifiers ──────────────
    public String getBrand() {
        return brand;
    }

    public int getYear() {
        return year;
    }

    public void accelerate(int amount) {
        this.speed += amount;
    }

    protected double getEngineCapacity() {
        return engineCapacity;
    }

    private boolean checkElectric() {
        return isElectric;
    }

    public String describe() {
        return String.format("%s (%d) — %.1fL — Electric: %b — Speed: %d km/h",
                brand, year, engineCapacity, isElectric, speed);
    }

    @Override
    public String toString() {
        return "Vehicle{ " + describe() + " }";
    }
}