package com.jbion.riseoflords;

/**
 * Parameters to choose players.
 */
public class PlayerFilter {

    /** The minimum rank to attack. */
    private final int minRank;

    /** The minimum rank to attack. */
    private final int maxRank;

    /** The maximum number of turns to spend. */
    private final int maxTurns;

    /** Only players with at least this much gold will be attacked. */
    private final int goldThreshold;

    /**
     * Creates a new {@link PlayerFilter} with default values.
     */
    public PlayerFilter() {
        this(2500, 4500, 50, 450000);
    }

    /**
     * Creates a new {@link PlayerFilter} with the specified values.
     * 
     * @param minRank
     *            lower limit for players rank
     * @param maxRank
     *            upper limit for players rank
     * @param maxTurns
     *            the maximum number of players to attack
     * @param goldThreshold
     *            lower limit for players gold
     */
    public PlayerFilter(int minRank, int maxRank, int maxTurns, int goldThreshold) {
        this.minRank = minRank;
        this.maxRank = maxRank;
        this.maxTurns = maxTurns;
        this.goldThreshold = goldThreshold;
    }

    public int getMinRank() {
        return minRank;
    }

    public int getMaxRank() {
        return maxRank;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public int getGoldThreshold() {
        return goldThreshold;
    }
}