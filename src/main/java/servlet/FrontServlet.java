package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import servlet.utils.MethodInvoker;
import servlet.utils.UrlRouter;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();
        UrlRouter routes = (UrlRouter) context.getAttribute("routes");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        MethodInvoker invoker = (routes != null) ? routes.findByUrl(path) : null;

        resp.setContentType("text/plain");

        if (invoker != null) {
            try {
                // 1 Récupérer tous les paramètres de la requête
                Map<String, String[]> params = req.getParameterMap();

                // 2 Récupérer la méthode à invoquer et ses paramètres
                Method method = invoker.getMethod();
                Parameter[] methodParameters = method.getParameters();
                Object[] args = new Object[methodParameters.length];

                // 3 Remplir le tableau args avec les valeurs converties
                for (int i = 0; i < methodParameters.length; i++) {
                    Parameter param = methodParameters[i];
                    String paramName = param.getName(); // nécessite javac -parameters

                    String[] rawValues = params.get(paramName);
                    String raw = (rawValues != null && rawValues.length > 0) ? rawValues[0] : null;

                    args[i] = convertType(raw, param.getType());
                }

                // 4 Appeler la méthode sur l'objet contrôleur
                Object invokedObject = invoker.execute(args);

                // 5 Gérer le retour
                if (invokedObject instanceof String) {
                    resp.getWriter().println(invokedObject);
                }

            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }

        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }

    // Conversion automatique des types simples
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

        // Ajouter d'autres types si nécessaire (long, float, etc.)
        return value; // fallback
    }
}
