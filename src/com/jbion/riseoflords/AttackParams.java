package com.jbion.riseoflords;

public class AttackParams {
    
    /** How many attacks before repairing the weapons. */
    private final int repairFrequency;
    
    /** How many attacks before storing the gold into the chest. */
    private final int storingFrequency;

    public AttackParams(int repairFrequency, int storingFrequency) {
        this.repairFrequency = repairFrequency;
        this.storingFrequency = storingFrequency;
    }

    public int getRepairFrequency() {
        return repairFrequency;
    }

    public int getStoringFrequency() {
        return storingFrequency;
    }
}
