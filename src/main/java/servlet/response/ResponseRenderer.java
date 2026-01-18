package servlet.response;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.ModelView;
import servlet.annotations.Json;
import servlet.annotations.Session;
import servlet.api.ApiResponse;
import servlet.session.CustomSession;
import servlet.utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class ResponseRenderer {

    private static final String SESSION_COOKIE_NAME = "FRAMEWORK_SESSION_ID"; // Changé pour éviter conflit avec Tomcat

    public void render(HttpServletResponse resp, Object result, HttpServletRequest req,
            ServletContext context, Method method, StringBuilder debug) throws Exception {

        // Gérer le cookie de session si une session a été utilisée
        handleSessionCookie(req, resp, method);

        boolean isJson = method.isAnnotationPresent(Json.class);

        if (isJson) {
            sendJson(resp, result, debug);
        } else if (result instanceof String) {
            sendString(resp, (String) result, debug);
        } else if (result instanceof ModelView) {
            forwardToModelView(req, resp, (ModelView) result, debug, context);
        } else {
            sendPlain(resp, result != null ? result.toString() : "null");
        }
    }

    /**
     * Crée ou met à jour le cookie de session si une session a été utilisée dans la
     * méthode
     */
    private void handleSessionCookie(HttpServletRequest req, HttpServletResponse resp, Method method) {
        // Vérifier si la méthode utilise @Session
        boolean hasSessionParam = false;
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Session.class)) {
                hasSessionParam = true;
                break;
            }
        }

        if (!hasSessionParam) {
            return;
        }

        // Récupérer la session depuis l'attribut de requête
        CustomSession session = (CustomSession) req.getAttribute("__current_session");

        if (session == null) {
            System.err.println(
                    "[ResponseRenderer] ATTENTION: Session utilisée mais non trouvée dans les attributs de requête");
            return;
        }

        // Toujours créer/mettre à jour le cookie pour maintenir la session
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, session.getSessionId());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
        sessionCookie.setMaxAge(session.getMaxInactiveInterval());

        resp.addCookie(sessionCookie);

        System.out.println("[ResponseRenderer] Cookie de session envoyé: " + session.getSessionId() +
                " (isNew=" + session.isNew() + ", path=" + sessionCookie.getPath() + ")");
    }

    public void notFound(HttpServletResponse resp, StringBuilder debug) throws IOException {
        sendPlain(resp, "404 - Aucun contrôleur trouvé", 404, debug);
    }

    private void sendJson(HttpServletResponse resp, Object result, StringBuilder debug) throws Exception {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ApiResponse<?> response = (result instanceof ApiResponse)
                    ? (ApiResponse<?>) result
                    : ApiResponse.success(result);
            resp.getWriter().print(JsonUtil.toJson(response));
        } catch (Exception e) {
            ApiResponse<?> err = ApiResponse.error(500, "Erreur de sérialisation JSON", e.getMessage());
            resp.getWriter().print(JsonUtil.toJson(err));
        }
    }

    private void sendString(HttpServletResponse resp, String text, StringBuilder debug) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();
        if (debug != null)
            out.println(debug.toString());
        out.println(text);
    }

    private void forwardToModelView(HttpServletRequest req, HttpServletResponse resp,
            ModelView mv, StringBuilder debug, ServletContext context) throws Exception {
        String viewPath = "/" + mv.getView();
        String realPath = context.getRealPath(viewPath);
        File viewFile = new File(realPath);

        if (viewFile.exists() && viewFile.isFile()) {
            if (debug != null)
                req.setAttribute("_debugOutput", debug.toString());
            mv.passVar(req);
            context.getRequestDispatcher(viewPath).forward(req, resp);
        } else {
            sendPlain(resp, "Vue introuvable : " + realPath, 404, debug);
        }
    }

    private void sendPlain(HttpServletResponse resp, String message) throws IOException {
        sendPlain(resp, message, 200, null);
    }

    private void sendPlain(HttpServletResponse resp, String message, int status, StringBuilder debug)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("text/plain;charset=UTF-8");
        var out = resp.getWriter();
        if (debug != null)
            out.println(debug.toString());
        out.println(message);
    }
}