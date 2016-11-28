package org.hildan.bots.riseoflords.config;

public class AttackParams {

    private final int maxTurns;

    private final int repairPeriod;

    private final int storageThreshold;

    /**
     * Creates a new AttackParams.
     *
     * @param maxTurns
     *            the maximum number of players to attack
     * @param repairPeriod
     *            number of attacks between each weapon reparation
     * @param storageThreshold
     *            gold will be stored if this amount is reached
     */
    AttackParams(int maxTurns, int repairPeriod, int storageThreshold) {
        this.maxTurns = maxTurns;
        this.repairPeriod = repairPeriod;
        this.storageThreshold = storageThreshold;
    }

    /**
     * Gets the max number of turns to use for the attack.
     *
     * @return the max number of turns to use for the attack
     */
    public int getMaxTurns() {
        return maxTurns;
    }

    /**
     * Gets the number of attacks between each weapon reparation.
     *
     * @return the number of attacks between each weapon reparation
     */
    public int getRepairPeriod() {
        return repairPeriod;
    }

    /**
     * Gets the amount of gold that should trigger storage in the chest.
     *
     * @return the amount of gold that should trigger storage in the chest.
     */
    public int getStorageThreshold() {
        return storageThreshold;
    }

    @Override
    public String toString() {
        return "Attack params:\n   maxTurns: " + maxTurns + "\n   repair period: " + repairPeriod
                + "\n   gold storage threshold: " + storageThreshold;
    }
}
