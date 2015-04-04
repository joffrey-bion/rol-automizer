package org.hildan.bots.riseoflords.config;

public class AttackParams {

    /** The maximum number of turns to spend. */
    private final int maxTurns;

    /** Number of attacks between each weapon reparation. */
    private final int repairPeriod;

    /** Number of attacks between each gold storage. */
    private final int storagePeriod;

    /**
     * Creates a new {@link AttackParams}.
     *
     * @param maxTurns
     *            the maximum number of players to attack
     * @param repairPeriod
     *            number of attacks between each weapon reparation
     * @param storagePeriod
     *            number of attacks between each gold storage
     */
    public AttackParams(int maxTurns, int repairPeriod, int storagePeriod) {
        this.maxTurns = maxTurns;
        this.repairPeriod = repairPeriod;
        this.storagePeriod = storagePeriod;
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
     * Gets the number of attacks between each gold storage.
     *
     * @return the number of attacks between each gold storage
     */
    public int getStoragePeriod() {
        return storagePeriod;
    }

    @Override
    public String toString() {
        return "Attack params:\n   maxTurns: " + maxTurns + "\n   repair period: " + repairPeriod
                + "\n   storage period: " + storagePeriod;
    }
}
