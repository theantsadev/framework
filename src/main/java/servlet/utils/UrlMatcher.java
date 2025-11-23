package servlet.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlMatcher {
    public static boolean match(String urlGen, String urlSpec) {
        String regex = urlGen.replaceAll("\\{[^/]+}", "([^/]+)");
        boolean b = urlSpec.matches(regex);
        return b;
    }

    public static String diff(String urlGen, String urlSpec) {
        // Transformer {id} → ([^/]+)
        String regex = urlGen.replaceAll("\\{([^/]+)}", "([^/]+)");

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(urlSpec);

        if (matcher.matches()) {
            // Groupe 1 = la valeur dynamique
            return matcher.group(1);
        }

        return null; // pas de match
    }

    public static Map<String, String> extractParams(String urlGen, String urlSpec) {
        String regex = urlGen.replaceAll("\\{[^/]+}", "([^/]+)");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(urlSpec);

        if (!matcher.matches())
            return null;

        // extraire les noms de paramètres
        String[] parts = urlGen.split("/");
        Map<String, String> map = new HashMap<>();
        int groupIndex = 1;

        for (String part : parts) {
            if (part.matches("\\{[^/]+}")) {
                String paramName = part.substring(1, part.length() - 1);
                map.put(paramName, matcher.group(groupIndex++));
            }
        }

        return map;
    }

}
