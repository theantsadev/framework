package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

import servlet.annotations.Controller;
import servlet.annotations.Url;
import servlet.utils.ClassDetector;
import servlet.utils.MethodInvoker;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();
        Map<String, MethodInvoker> routes = (Map<String, MethodInvoker>) context.getAttribute("routes");

        String path = req.getRequestURI().substring(req.getContextPath().length());
        MethodInvoker invoker = (routes != null) ? routes.get(path) : null;

        resp.setContentType("text/plain");

        if (invoker != null) {
            try {
                Object controller = invoker.getControllerClass().getDeclaredConstructor().newInstance();
                Object result = invoker.getMethod().invoke(controller);
                if (result instanceof String) {
                    String string = (String) result;
                    resp.getWriter().print(string);
                }
            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }
        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }
}
