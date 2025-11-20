package servlet.utils;

import java.util.HashMap;
import java.util.Map;

public class UrlRouter extends HashMap<String, MethodInvoker> {
    public MethodInvoker findByUrl(String urlSpec) {

        for (Map.Entry<String, MethodInvoker> entry : this.entrySet()) {
            String urlGen = entry.getKey();
            MethodInvoker value = entry.getValue();
            if (UrlMatcher.match(urlGen, urlSpec)) {
                return value;
            }
        }
        return null;
    }
}
