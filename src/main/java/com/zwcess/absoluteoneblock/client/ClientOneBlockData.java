package com.zwcess.absoluteoneblock.client;

public class ClientOneBlockData {
    private static int blocksBroken;
    private static int blocksNeeded;
    private static String currentPhaseName = "";
    private static String nextPhaseName = "";

    public static void set(int broken, int needed, String currentPhase, String nextPhase) {
        ClientOneBlockData.blocksBroken = broken;
        ClientOneBlockData.blocksNeeded = needed;
        ClientOneBlockData.currentPhaseName = currentPhase;
        ClientOneBlockData.nextPhaseName = nextPhase;
    }

    public static int getBlocksBroken() {
        return blocksBroken;
    }

    public static int getBlocksNeeded() {
        return blocksNeeded;
    }

    public static String getCurrentPhaseName() {
        return currentPhaseName != null ? currentPhaseName : "Loading...";
    }

    public static String getNextPhaseName() {
        return nextPhaseName != null ? nextPhaseName : "???";
    }
}
