package hu.uni_obuda.thesis.railways.data.delaydatacollector.util;

public class StringUtils {

    public static boolean isText(String text) {
        return text != null && !text.isBlank();
    }

    public static boolean isEachText(String... texts) {
        if (texts == null) return false;
        for (String text : texts) {
            if (!isText(text)) return false;
        }
        return true;
    }

    public static boolean isAnyText(String... texts) {
        if (texts == null) return false;
        for (String text : texts) {
            if (isText(text)) return true;
        }
        return false;
    }
}
