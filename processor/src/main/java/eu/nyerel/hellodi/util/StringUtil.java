package eu.nyerel.hellodi.util;

import lombok.NonNull;

/**
 * @author Rastislav Papp (rastislav.papp@gmail.com)
 */
public class StringUtil {

    private StringUtil() {}

    public static String makeFirstLetterLowerCase(@NonNull String s) {
        return s.isEmpty() ? "" : s.substring(0, 1).toLowerCase() + s.substring(1);
    }

}
