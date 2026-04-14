package com.example.net;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        NetworkPacket packet = new NetworkPacket();
        System.out.println("After construction: " + packet);

        Thread.sleep(100);

        packet.refresh();
        System.out.println("After refresh: " + packet);
    }
}
