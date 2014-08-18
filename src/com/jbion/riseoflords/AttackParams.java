package com.jbion.riseoflords;

/**
 * Parameters to attack players.
 */
public class AttackParams {
    
    /** The player to start from. */
    private int startRank = 2500;
    
    /** The player to stop at. */
    private int endRank = 4500;
    
    /** The number of turns to spend. */
    private int nbTurns = 50;
    
    /** Only players with at least this much gold will be attacked. */
    private int goldThreshold = 450000;
    
    /** How many attacks before repairing the weapons. */
    private int repairFrequency = 5;
    
    /** How many attacks before storing the gold into the chest. */
    private int storingFrequency = 3;

    public AttackParams start(int rank) {
        this.startRank = rank;
        return this;
    }

    public AttackParams end(int rank) {
        this.endRank = rank;
        return this;
    }

    public AttackParams turns(int n) {
        this.nbTurns = n;
        return this;
    }

    public AttackParams goldThreshold(int amount) {
        this.goldThreshold = amount;
        return this;
    }

    public AttackParams repairFreq(int n) {
        this.repairFrequency = n;
        return this;
    }

    public AttackParams storingFreq(int n) {
        this.storingFrequency = n;
        return this;
    }
    
    public int getStartRank() {
        return startRank;
    }
    
    public int getEndRank() {
        return endRank;
    }
    
    public int getNbTurns() {
        return nbTurns;
    }

    public int getGoldThreshold() {
        return goldThreshold;
    }

    public int getRepairFrequency() {
        return repairFrequency;
    }

    public int getStoringFrequency() {
        return storingFrequency;
    }

    public void decrementTurns(int n) {
        this.nbTurns -= n;
    }

    public void nextPage() {
        this.startRank += 99;
    }
}