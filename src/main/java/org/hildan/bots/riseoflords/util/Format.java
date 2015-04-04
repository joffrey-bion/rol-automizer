package org.hildan.bots.riseoflords.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Format {

    private static final DecimalFormat fmt;
    static {
        final DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('.');
        fmt = new DecimalFormat("###,###.##", symbols);
    }

    public static String gold(int amount) {
        return fmt.format(amount);
    }

}
