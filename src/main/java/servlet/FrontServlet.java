package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import servlet.annotations.RequestParam;
import servlet.utils.MethodInvoker;
import servlet.utils.UrlRouter;

public class FrontServlet extends HttpServlet {

    private static final boolean DEBUG = true; // Active/Désactive le debug

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();
        UrlRouter routes = (UrlRouter) context.getAttribute("routes");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        MethodInvoker invoker = (routes != null) ? routes.findByUrl(path) : null;

        resp.setContentType("text/plain");

        if (DEBUG) {
            System.out.println("\n========== DEBUG FrontServlet ==========");
            System.out.println("URL appelée : " + path);
            System.out.println("Route trouvée : " + (invoker != null));
        }

        if (invoker != null) {
            try {
                Map<String, String[]> params = req.getParameterMap();

                if (DEBUG) {
                    System.out.println("\n-- Paramètres HTTP reçus --");
                    params.forEach((k, v) -> System.out.println(k + " = " + String.join(",", v)));
                }

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

                    if (param.isAnnotationPresent(RequestParam.class)) {
                        RequestParam reqParam = param.getAnnotation(RequestParam.class);
                        paramName = reqParam.name();
                    }

                    // javac -parameters permet d'utiliser le nom réel de l'arg si name=""
                    if (paramName == null || paramName.isEmpty()) {
                        paramName = param.getName();
                    }

                    String[] rawValues = params.get(paramName);
                    String raw = (rawValues != null && rawValues.length > 0) ? rawValues[0] : null;

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

                Object invokedObject = invoker.execute(args);

                if (DEBUG) {
                    System.out.println("\n-- Retour méthode --");
                    System.out.println("Valeur retournée: " + invokedObject);
                }

                if (invokedObject instanceof String) {
                    resp.getWriter().println(invokedObject);
                }

                if (DEBUG) {
                    System.out.println("=======================================\n");
                }

            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }

        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }

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

        return value;
    }
}
