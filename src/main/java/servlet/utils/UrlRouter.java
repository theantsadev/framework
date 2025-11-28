package servlet.utils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import servlet.annotations.mapping.GetMapping;
import servlet.annotations.mapping.PostMapping;

public class UrlRouter extends HashMap<String, MethodInvoker> {
    public Class<? extends Annotation> getAnnotationByMethod(String httpMethod) {
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return GetMapping.class;
        } else if ("POST".equalsIgnoreCase(httpMethod)) {
            return PostMapping.class;
        }
        return null;
    }

    public RouteMatch findByUrl(String urlSpec, String httpMethod) {

        Class<? extends Annotation> httpAnnotation = getAnnotationByMethod(httpMethod); // GetMapping.class ou
                                                                                        // PostMapping.class

        for (Map.Entry<String, MethodInvoker> entry : this.entrySet()) {

            String urlPattern = entry.getKey();
            MethodInvoker invoker = entry.getValue();

            // Étape 1 : Vérifier si l’URL correspond
            if (!UrlMatcher.match(urlPattern, urlSpec)) {
                continue;
            }

            // Étape 2 : Vérifier si la méthode correspond au verbe HTTP
            if (httpAnnotation != null && !invoker.getMethod().isAnnotationPresent(httpAnnotation)) {
                continue;
            }

            // Étape 3 : Renvoyer la bonne route
            RouteMatch match = new RouteMatch();
            match.setMethod(invoker);

            Map<String, String> params = UrlMatcher.extractParams(urlPattern, urlSpec);
            if (params != null) {
                match.setPathParams(params);
            }

            return match;
        }

        // Aucun route trouvée
        return null;
    }

}
