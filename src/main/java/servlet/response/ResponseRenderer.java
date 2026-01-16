package servlet.response;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import servlet.ModelView;
import servlet.annotations.Json;
import servlet.annotations.Upload;
import servlet.api.ApiResponse;
import servlet.utils.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class ResponseRenderer {

    public void render(HttpServletResponse resp, Object result, HttpServletRequest req,
            ServletContext context, Method method, StringBuilder debug) throws Exception {

        boolean isUpload = method.isAnnotationPresent(Upload.class);
        if (isUpload) {
            Upload uploadAnno = method.getAnnotation(Upload.class);
            String uploadDir = uploadAnno.directory();
            String uploadPath = context.getRealPath("/") + File.separator + uploadDir;
            File uploadDirectory = new File(uploadPath);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }
            for (Part part : req.getParts()) {
                String fileName = part.getSubmittedFileName();
                if (fileName != null) {
                    part.write(uploadPath + File.separator + fileName);
                }
            }

        }

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