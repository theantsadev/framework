package servlet.utils;

public class UrlMatcher {
    public static boolean match(String urlGen, String urlSpec) {
        String regex = urlGen.replaceAll("\\{[^/]+}", "([^/]+)");
        boolean b = urlSpec.matches(regex);
        return b;
    }
}
