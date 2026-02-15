package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.binding.ArgumentResolver;
import servlet.response.ResponseRenderer;
import servlet.security.ForbiddenException;
import servlet.security.SecurityChecker;
import servlet.security.UnauthorizedException;
import servlet.session.CustomSession;
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
    private SecurityChecker securityChecker;

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
        securityChecker = new SecurityChecker();

        // Vérifier la présence de Jackson pour les fonctionnalités JSON
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Class.forName("com.fasterxml.jackson.annotation.JsonInclude");
        } catch (ClassNotFoundException e) {
            context.log(
                    "[Framework] ATTENTION: Jackson non trouvé sur le classpath. Si vous utilisez JsonUtil, ApiResponse ou ErrorInfo, ajoutez les dépendances suivantes : jackson-annotations-2.20.jar, jackson-core-2.20.1.jar, jackson-databind-2.20.1.jar");
        }
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
        CustomSession session = null;
        for (Object arg : args) {
            if (arg instanceof CustomSession) {
                session = (CustomSession) arg;
                req.setAttribute("__current_session", arg);
                break;
            }
        }

        // Vérifier les autorisations (@Authorized, @Role)
        Method method = routeMatch.getMethodInvoker().getMethod();
        securityChecker.checkAuthorization(method, session);

        if (debug != null) {
            debug.append("Sécurité     : OK\n");
        }

        Object result = routeMatch.getMethodInvoker().execute(args);

        responseRenderer.render(resp, result, req, getServletContext(),
                routeMatch.getMethodInvoker().getMethod(), debug);
    }

    private void handleException(HttpServletResponse resp, Exception e, Method method, StringBuilder debug) {
        try {
            int statusCode = 500;
            String errorType = "ERREUR";

            // Gestion des exceptions de sécurité
            if (e instanceof UnauthorizedException) {
                statusCode = 401;
                errorType = "NON AUTHENTIFIÉ";
            } else if (e instanceof ForbiddenException) {
                statusCode = 403;
                errorType = "ACCÈS INTERDIT";
            }

            resp.setStatus(statusCode);
            resp.setContentType("text/plain;charset=UTF-8");
            var out = resp.getWriter();
            if (debug != null)
                out.println(debug);
            out.println("\n========== " + errorType + " (" + statusCode + ") ==========");
            out.println(e.getMessage());
            if (statusCode == 500) {
                e.printStackTrace(out);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}