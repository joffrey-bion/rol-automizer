package org.hildan.bots.riseoflords.model;

public enum Alignment {

    SAINT("Sai."),
    CHEVALERESQUE("Che."),
    ALTRUISTE("Alt."),
    JUSTE("Jus."),
    NEUTRE("Neu."),
    SANS_SCRUPULES("SsS."),
    VIL("Vil."),
    ABOMINABLE("Abo."),
    DEMONIAQUE("D\u00e9m.");

    // for efficiency
    private static final Alignment[] ALIGNMENTS = values();

    private final String shortName;

    Alignment(String shortName) {
        this.shortName = shortName;
    }

    public static Alignment get(String shortName) {
        for (final Alignment al : ALIGNMENTS) {
            if (al.shortName.equals(shortName)) {
                return al;
            }
        }
        throw new IllegalArgumentException("No alignment corresponds to the short name '" + shortName + "'");
    }
}
