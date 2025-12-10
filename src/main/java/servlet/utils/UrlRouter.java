package servlet.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrlRouter extends HashMap<String, List<RegisteredRoute>> {

    public void addRoute(String urlPattern, String httpMethod, MethodInvoker invoker) {

        this.computeIfAbsent(urlPattern, k -> new ArrayList<>())
                .add(new RegisteredRoute(httpMethod, invoker));
    }

    public RouteMatch findByUrl(String urlSpec, String httpMethod) {

        for (Map.Entry<String, List<RegisteredRoute>> entry : this.entrySet()) {

            String urlPattern = entry.getKey();

            // 1) Check URL matching
            if (!UrlMatcher.match(urlPattern, urlSpec)) {
                continue;
            }

            List<RegisteredRoute> registered = entry.getValue();

            for (RegisteredRoute route : registered) {

                // 2) Check HTTP method
                if (!route.getHttpMethod().equalsIgnoreCase(httpMethod)) {
                    System.out.println(route.getHttpMethod());
                    continue;
                }

                // 3) Cette route matche parfaitement
                RouteMatch match = new RouteMatch();
                match.setMethod(route.getInvoker());

                Map<String, String> params = UrlMatcher.extractParams(urlPattern, urlSpec);
                match.setPathParams(params);

                return match;
            }
        }

        return null;
    }
}
