package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.File;
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

        if (invoker != null) {
            try {
                Object controller = invoker.getControllerClass().getDeclaredConstructor().newInstance();
                Object result = invoker.getMethod().invoke(controller);

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
                    System.out.println("Vue recherchée : " + realPath);

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

            } catch (Exception e) {
                e.printStackTrace(resp.getWriter());
            }
        } else {
            resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
        }
    }
}
