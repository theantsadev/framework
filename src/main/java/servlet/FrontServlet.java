package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

import servlet.annotations.Controller;
import servlet.annotations.Url;
import servlet.utils.ClassDetector;
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
                String methodName = invoker.getMethod().getName();
                resp.getWriter().print("Url enregistré : " + path + " -> " + methodName);
            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }
        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }
}
