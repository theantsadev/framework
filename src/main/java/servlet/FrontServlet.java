package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.binding.ArgumentResolver;
import servlet.response.ResponseRenderer;
import servlet.utils.RouteMatch;
import servlet.utils.UrlRouter;

import java.io.IOException;
import java.lang.reflect.Method;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, // 1 MB
        maxFileSize = 1024 * 1024 * 10, // 10 MB
        maxRequestSize = 1024 * 1024 * 50 // 50 MB
)
public class FrontServlet extends HttpServlet {

    private UrlRouter routes;
    private ArgumentResolver argumentResolver;
    private ResponseRenderer responseRenderer;

    private static final boolean DEBUG = true;

    @Override
    public void init() {
        ServletContext context = getServletContext();
        routes = (UrlRouter) context.getAttribute("routes");
        if (routes == null) {
            throw new IllegalStateException("Routes non initialisées");
        }
        argumentResolver = new ArgumentResolver();
        responseRenderer = new ResponseRenderer();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        StringBuilder debugOutput = DEBUG ? new StringBuilder()
                .append("\n========== DEBUG FrontServlet ==========\n")
                .append("HTTP Method : ").append(req.getMethod()).append("\n")
                .append("URL         : ").append(req.getRequestURI()).append("\n") : null;

        try {
            processRequest(req, resp, debugOutput);
        } catch (Exception e) {
            handleException(resp, e, null, debugOutput);
        }
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp, StringBuilder debug)
            throws Exception {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        RouteMatch routeMatch = routes.findByUrl(path, req.getMethod());

        if (routeMatch == null) {
            responseRenderer.notFound(resp, debug);
            return;
        }

        if (debug != null) {
            debug.append("Route trouvée  : ")
                    .append(routeMatch.getMethodInvoker().getMethod().getDeclaringClass().getSimpleName())
                    .append(".").append(routeMatch.getMethodInvoker().getMethod().getName()).append("\n");
        }

        Object[] args = argumentResolver.resolve(routeMatch, req, debug);

        // Stocker la session dans la requête pour le ResponseRenderer
        for (Object arg : args) {
            if (arg instanceof servlet.session.CustomSession) {
                req.setAttribute("__current_session", arg);
                break;
            }
        }

        Object result = routeMatch.getMethodInvoker().execute(args);

        responseRenderer.render(resp, result, req, getServletContext(),
                routeMatch.getMethodInvoker().getMethod(), debug);
    }

    private void handleException(HttpServletResponse resp, Exception e, Method method, StringBuilder debug) {
        try {
            resp.setStatus(500);
            resp.setContentType("text/plain;charset=UTF-8");
            var out = resp.getWriter();
            if (debug != null)
                out.println(debug);
            out.println("\n========== ERREUR ==========");
            e.printStackTrace(out);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}