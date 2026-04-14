import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        Entity player = new Entity(
            42,
            new byte[]{/* mesh data */},
            "/textures/player.png",
            3.0, 4.0, 0.0   // posX=3, posY=4, posZ=0
        );

        double centerX = 0.0, centerY = 0.0, radius = 10.0;

        // APPROACH B — recommended call
        boolean safe = player.isInSafetyZoneSecure(
            3.0, 4.0,          // only posX, posY extracted in Java
            centerX, centerY,
            radius
        );

        // Distance = hypot(3,4) = 5.0 — which is <= 10.0, so: true
        System.out.println("Entity in safety zone: " + safe); // true
    }
}