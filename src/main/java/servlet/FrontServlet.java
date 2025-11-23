package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import servlet.annotations.RequestParam;
import servlet.utils.MethodInvoker;
import servlet.utils.RouteMatch;
import servlet.utils.UrlRouter;

public class FrontServlet extends HttpServlet {

    private static final boolean DEBUG = true; // true pour activer debug

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();
        UrlRouter routes = (UrlRouter) context.getAttribute("routes");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        RouteMatch routeMatch = (routes != null) ? routes.findByUrl(path) : null;

        resp.setContentType("text/plain");

        if (routeMatch == null) {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
            return;
        }

        MethodInvoker invoker = routeMatch.getMethodInvoker();
        Map<String, String> pathParams = routeMatch.getPathParams();
        Map<String, String[]> queryParams = req.getParameterMap();

        if (DEBUG) {
            System.out.println("\n========== DEBUG FrontServlet ==========");
            System.out.println("URL appelée       : " + path);
            System.out.println("Route trouvée     : " + (invoker != null));
            System.out.println("Paramètres PATH   : " + pathParams);
            System.out.println("Paramètres QUERY  : " + queryParams);
        }

        try {
            Method method = invoker.getMethod();
            Parameter[] methodParameters = method.getParameters();
            Object[] args = new Object[methodParameters.length];

            if (DEBUG) {
                System.out.println("\n-- Méthode cible --");
                System.out.println("Classe : " + method.getDeclaringClass().getName());
                System.out.println("Méthode : " + method.getName());
            }

            for (int i = 0; i < methodParameters.length; i++) {
                Parameter param = methodParameters[i];
                String paramName = null;

                // 1 Vérifier annotation RequestParam
                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam reqParam = param.getAnnotation(RequestParam.class);
                    paramName = reqParam.name();
                }

                // 2 Si annotation vide ou absente → utiliser nom réel
                if (paramName == null || paramName.isEmpty()) {
                    paramName = param.getName();
                }

                // 3 Récupérer valeur : priorité PATH, sinon QUERY
                String raw = null;
                if (pathParams != null && pathParams.containsKey(paramName)) {
                    raw = pathParams.get(paramName);
                } else if (queryParams != null && queryParams.containsKey(paramName)) {
                    String[] rawValues = queryParams.get(paramName);
                    raw = (rawValues != null && rawValues.length > 0) ? rawValues[0] : null;
                }

                // 4 Conversion automatique
                Object converted = convertType(raw, param.getType());
                args[i] = converted;

                if (DEBUG) {
                    System.out.println("\nParamètre #" + i);
                    System.out.println("  Nom Java        : " + param.getName());
                    System.out.println("  Nom attendu     : " + paramName);
                    System.out.println("  Type            : " + param.getType().getSimpleName());
                    System.out.println("  Valeur brute    : " + raw);
                    System.out.println("  Valeur convertie: " + converted);
                }
            }

            // 5 Exécution de la méthode
            Object invokedObject = invoker.execute(args);

            if (DEBUG) {
                System.out.println("\n-- Retour méthode --");
                System.out.println("Valeur retournée: " + invokedObject);
                System.out.println("=======================================\n");
            }

            resp.getWriter().println(invokedObject);

        } catch (Exception e) {
            e.printStackTrace(resp.getWriter());
        }
    }

    // Conversion automatique pour types simples
    private Object convertType(String value, Class<?> type) {
        if (value == null)
            return null;

        if (type == String.class)
            return value;
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(value);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(value);

        return value; // fallback
    }
}
