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
                Map<String, String[]> params = req.getParameterMap();
                Method method = invoker.getMethod();
                Parameter[] methodParameters = method.getParameters();
                for (Parameter parameter : methodParameters) {
                    for (Map.Entry<String, String[]> entry : params.entrySet()) {
                        String key = entry.getKey();
                        String[] values = entry.getValue();
                        
                        if (parameter.getName().equals(key)) {
                            resp.getWriter()
                                    .println("le parametre " + key + " existe dans la methode " + method.getName());
                        }
                    }
                }
                String methodName = invoker.getMethod().getName();
                resp.getWriter().println("Url enregistré : " + path + " -> " + methodName);
            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }
        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }
}
