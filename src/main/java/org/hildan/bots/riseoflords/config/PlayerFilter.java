package org.hildan.bots.riseoflords.config;

/**
 * Parameters to choose players.
 */
public class PlayerFilter {

    /** The minimum rank to attack. */
    private final int minRank;

    /** The maximum rank to attack. */
    private final int maxRank;

    /** Only players with at least this much gold will be attacked. */
    private final int goldThreshold;

    /**
     * Creates a new {@link PlayerFilter} with default values.
     */
    public PlayerFilter() {
        this(2500, 4500, 450000);
    }

    /**
     * Creates a new {@link PlayerFilter} with the specified values.
     *
     * @param minRank
     *            lower limit for players rank
     * @param maxRank
     *            upper limit for players rank
     * @param goldThreshold
     *            lower limit for players gold
     */
    public PlayerFilter(int minRank, int maxRank, int goldThreshold) {
        this.minRank = minRank;
        this.maxRank = maxRank;
        this.goldThreshold = goldThreshold;
    }

    /**
     * Gets the smallest accepted rank.
     *
     * @return the smallest accepted rank
     */
    public int getMinRank() {
        return minRank;
    }

    /**
     * Gets the largest accepted rank.
     *
     * @return the largest accepted rank
     */
    public int getMaxRank() {
        return maxRank;
    }

    /**
     * Gets the minimum accepted amount of gold.
     *
     * @return the minimum accepted amount of gold
     */
    public int getGoldThreshold() {
        return goldThreshold;
    }

    @Override
    public String toString() {
        return "Player filter:\n   ranks: " + minRank + "-" + maxRank + "\n   min gold: " + goldThreshold;
    }

    public int getNbPlayersToScan() {
        return getMaxRank() - getMinRank() + 1;
    }
}