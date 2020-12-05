package org.hildan.bots.riseoflords.model;

public class Player {

    private int rank;
    private String name;
    private int gold;
    private Army army;
    private Alignment alignment;

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public Army getArmy() {
        return army;
    }

    public void setArmy(Army army) {
        this.army = army;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    @Override
    public String toString() {
        return "{rank=" + rank + ", name=" + name + ", gold=" + gold + ", army=" + army + ", align=" + alignment + "}";
    }
}
