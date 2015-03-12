package com.jbion.riseoflords.config;

public class AttackParams {

    /** The maximum number of turns to spend. */
    private final int maxTurns;

    /** Number of attacks between each weapon reparation. */
    private final int repairFrequency;

    /** Number of attacks between each gold storage. */
    private final int storingFrequency;

    /**
     * Creates a new {@link AttackParams}.
     *
     * @param maxTurns
     *            the maximum number of players to attack
     * @param repairFrequency
     *            number of attacks between each weapon reparation
     * @param storingFrequency
     *            number of attacks between each gold storage
     */
    public AttackParams(int maxTurns, int repairFrequency, int storingFrequency) {
        this.maxTurns = maxTurns;
        this.repairFrequency = repairFrequency;
        this.storingFrequency = storingFrequency;
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
    public int getRepairFrequency() {
        return repairFrequency;
    }

    /**
     * Gets the number of attacks between each gold storage.
     *
     * @return the number of attacks between each gold storage
     */
    public int getStoringFrequency() {
        return storingFrequency;
    }

    @Override
    public String toString() {
        return "Attack params:\n   maxTurns: " + maxTurns + "\n   repair freq: " + repairFrequency
                + "\n   storing freq: " + storingFrequency;
    }
}
