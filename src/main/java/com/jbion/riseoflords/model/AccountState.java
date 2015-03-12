package com.jbion.riseoflords.model;

public class AccountState {

    public int gold;
    public int chestGold;
    public int mana;
    public int adventurins;
    public int turns;

    @Override
    public String toString() {
        return "gold=" + gold + " chest=" + chestGold + " turns=" + turns;
    }
}
