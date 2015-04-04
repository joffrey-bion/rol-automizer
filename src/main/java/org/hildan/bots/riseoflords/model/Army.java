package org.hildan.bots.riseoflords.model;

public enum Army {

    WARRIORS("Chev.", "Guer."),
    MAGES("Sorc.", "Sorc."),
    SUICIDERS("Suic.", "Suic."),
    HEALERS("Sage", "Pr\u00e9t.");

    // for efficiency
    private static final Army[] ARMIES = values();

    private final String shortNameMan;
    private final String shortNameWoman;

    private Army(String shortNameMan, String shortNameWoman) {
        this.shortNameMan = shortNameMan;
        this.shortNameWoman = shortNameWoman;
    }

    public static Army get(String shortName) {
        for (final Army army : ARMIES) {
            if (army.shortNameMan.equals(shortName) || army.shortNameWoman.equals(shortName)) {
                return army;
            }
        }
        throw new IllegalArgumentException("No army corresponds to the short name '" + shortName + "'");
    }
}
