package servlet.utils;

import java.util.HashMap;
import java.util.Map;

public class UrlRouter extends HashMap<String, MethodInvoker> {

    public RouteMatch findByUrl(String urlSpec) {

        for (Map.Entry<String, MethodInvoker> entry : this.entrySet()) {
            String urlGen = entry.getKey();
            MethodInvoker value = entry.getValue();

            Map<String, String> diff = UrlMatcher.extractParams(urlGen, urlSpec);
            if (diff != null) {
                return new RouteMatch(value, diff);
            }
        }
        return null;
    }
}


