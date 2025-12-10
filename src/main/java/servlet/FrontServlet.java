package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.File;
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
        
        String httpMethod = req.getMethod();
        String path = req.getRequestURI().substring(req.getContextPath().length());
        RouteMatch routeMatch = (routes != null) ? routes.findByUrl(path, httpMethod) : null;

        if (DEBUG) {
            System.out.println("\n========== DEBUG FrontServlet ==========");
            System.out.println("Routes totales    : " + (routes != null ? routes.size() : 0));
            System.out.println("HTTP Method       : " + httpMethod);
            System.out.println("URL appelée       : " + path);
        }

        if (routeMatch == null) {
            resp.setContentType("text/plain");
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
            return;
        }

        MethodInvoker invoker = routeMatch.getMethodInvoker();
        Map<String, String> pathParams = routeMatch.getPathParams();
        Map<String, String[]> queryParams = req.getParameterMap();

        if (DEBUG) {
            System.out.println("Route trouvée     : " + invoker.getMethod().getName());
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

            // Injection des paramètres
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
            Object result = invoker.execute(args);

            if (DEBUG) {
                System.out.println("\n-- Retour méthode --");
                System.out.println("Valeur retournée: " + result);
                System.out.println("Type de retour  : " + (result != null ? result.getClass().getSimpleName() : "null"));
                System.out.println("=======================================\n");
            }

            // 6 Traitement du retour
            if (result instanceof String) {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().println(result);
                return;
            }

            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                String view = mv.getView();

                // Exemple : "test/pages/hello.jsp"
                String viewPath = "/" + view; // chemin relatif au contexte

                String realPath = context.getRealPath(viewPath);
                
                if (DEBUG) {
                    System.out.println("\n-- Résolution de vue --");
                    System.out.println("Vue demandée    : " + view);
                    System.out.println("ViewPath        : " + viewPath);
                    System.out.println("RealPath        : " + realPath);
                }

                File jspFile = new File(realPath);
                if (jspFile.exists() && jspFile.isFile()) {
                    // Transfert de la requête à Tomcat (servlet par défaut ou JSP compiler)
                    RequestDispatcher rd = context.getRequestDispatcher(viewPath);
                    mv.passVar(req);
                    rd.forward(req, resp);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Vue introuvable : " + realPath);
                }
                return;
            }

            // Fallback si type de retour inconnu
            resp.setContentType("text/plain");
            resp.getWriter().println(result);

        } catch (Exception e) {
            resp.setContentType("text/plain");
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