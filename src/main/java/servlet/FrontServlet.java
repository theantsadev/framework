package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import servlet.annotations.Json;
import servlet.annotations.RequestParam;
import servlet.api.ApiResponse;
import servlet.utils.*;

public class FrontServlet extends HttpServlet {

    private static final boolean DEBUG = true;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        StringBuilder debugOutput = new StringBuilder();

        if (DEBUG) {
            debugOutput.append("\n========== DEBUG FrontServlet ==========\n");
            debugOutput.append("HTTP Method : ").append(req.getMethod()).append("\n");
            debugOutput.append("URL         : ").append(req.getRequestURI()).append("\n");
        }

        try {
            processRequest(req, resp, debugOutput);
        } catch (Exception e) {
            handleException(req, resp, e, null, debugOutput);
        }
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp, StringBuilder debugOutput)
            throws Exception {

        ServletContext context = getServletContext();
        UrlRouter routes = (UrlRouter) context.getAttribute("routes");

        if (routes == null) {
            sendPlain(resp, "Erreur interne : routes non initialisées dans le ServletContext.", 500);
            return;
        }

        String httpMethod = req.getMethod();
        String path = req.getRequestURI().substring(req.getContextPath().length());
        RouteMatch routeMatch = routes.findByUrl(path, httpMethod);

        if (DEBUG) {
            debugOutput.append("Routes totales : ").append(routes.size()).append("\n");
            debugOutput.append("Recherche      : ").append(httpMethod).append(" ").append(path).append("\n");
        }

        if (routeMatch == null) {
            sendPlain(resp, "404 - Aucun contrôleur trouvé pour " + path, 404);
            return;
        }

        MethodInvoker invoker = routeMatch.getMethodInvoker();
        Method method = invoker.getMethod();
        Map<String, String> pathParams = routeMatch.getPathParams();
        Map<String, String[]> queryParams = req.getParameterMap();

        if (DEBUG) {
            debugOutput.append("Route trouvée  : ")
                    .append(method.getDeclaringClass().getSimpleName())
                    .append(".").append(method.getName()).append("\n");
            debugOutput.append("Path params    : ").append(pathParams).append("\n");
            debugOutput.append("Query params   : ").append(queryParams).append("\n");
        }

        Object[] args = buildMethodArguments(method, pathParams, queryParams, debugOutput);
        Object result = invoker.execute(args);

        boolean isJsonEndpoint = method.isAnnotationPresent(Json.class);

        if (isJsonEndpoint) {
            sendJsonResponse(resp, result);
        } else if (result instanceof String) {
            sendStringResponse(resp, (String) result, debugOutput);
        } else if (result instanceof ModelView) {
            forwardToModelView(req, resp, (ModelView) result, debugOutput, context);
        } else {
            sendPlain(resp, result != null ? result.toString() : "null");
        }
    }

    private Object[] buildMethodArguments(Method method,
            Map<String, String> pathParams,
            Map<String, String[]> queryParams,
            StringBuilder debug) throws Exception {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = getParameterName(param);

            // 1. Map<String, Object> complet
            if (Map.class.isAssignableFrom(param.getType())) {
                args[i] = queryParams.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
                continue;
            }

            // 2. Objet custom (simple ou tableau)
            if (isCustomObject(param.getType())) {
                if (param.getType().isArray()) {
                    args[i] = bindObjectArray(param.getType(), paramName, queryParams, debug);
                } else {
                    args[i] = bindObject(param.getType(), paramName, queryParams, debug);
                }
                continue;
            }

            // 3. Paramètre simple (path > query)
            String rawValue = pathParams != null && pathParams.containsKey(paramName)
                    ? pathParams.get(paramName)
                    : (queryParams.containsKey(paramName) ? queryParams.get(paramName)[0] : null);

            args[i] = convertType(rawValue, param.getType());

            if (DEBUG) {
                debug.append("Param #").append(i)
                        .append(" [").append(paramName)
                        .append("]  ").append(rawValue)
                        .append("  ").append(args[i])
                        .append(" (").append(param.getType().getSimpleName()).append(")\n");
            }
        }
        return args;
    }

    private String getParameterName(Parameter param) {
        RequestParam annotation = param.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return param.getName();
    }

    private void sendJsonResponse(HttpServletResponse resp, Object result) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            ApiResponse<?> response = (result instanceof ApiResponse)
                    ? (ApiResponse<?>) result
                    : ApiResponse.success(result);

            out.print(JsonUtil.toJson(response));
        } catch (Exception e) {
            ApiResponse<?> err = ApiResponse.error(500, "Erreur de sérialisation JSON", e.getMessage());
            try {
                out.print(JsonUtil.toJson(err));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if (DEBUG)
                e.printStackTrace();
        }
    }

    private void sendStringResponse(HttpServletResponse resp, String text, StringBuilder debug) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        if (DEBUG)
            out.println(debug.toString());
        out.println(text);
    }

    private void forwardToModelView(HttpServletRequest req, HttpServletResponse resp,
            ModelView mv, StringBuilder debug,
            ServletContext context) throws Exception {

        String viewPath = "/" + mv.getView();
        String realPath = context.getRealPath(viewPath);
        File viewFile = new File(realPath);

        if (viewFile.exists() && viewFile.isFile()) {
            if (DEBUG)
                req.setAttribute("_debugOutput", debug.toString());
            mv.passVar(req);
            context.getRequestDispatcher(viewPath).forward(req, resp);
        } else {
            sendPlain(resp, "Vue introuvable : " + realPath, 404);
        }
    }

    private void handleException(HttpServletRequest req, HttpServletResponse resp,
            Exception e, Method method, StringBuilder debug) {

        boolean isJson = method != null && method.isAnnotationPresent(Json.class);

        if (isJson) {
            try {
                resp.setContentType("application/json;charset=UTF-8");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                ApiResponse<?> err = ApiResponse.error(500, "Erreur interne du serveur",
                        DEBUG ? e.getMessage() : null);
                resp.getWriter().print(JsonUtil.toJson(err));
            } catch (Exception ex) {
                try {
                    resp.getWriter().print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur critique\"}");
                } catch (Exception ignored) {
                }
            }
        } else {
            try {
                resp.setContentType("text/plain;charset=UTF-8");
                PrintWriter out = resp.getWriter();
                if (DEBUG && debug != null)
                    out.println(debug.toString());
                out.println("\n========== ERREUR ==========");
                e.printStackTrace(out);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (DEBUG)
            e.printStackTrace();
    }

    private void sendPlain(HttpServletResponse resp, String message, int status) throws IOException {
        resp.setStatus(status);
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println(message);
    }

    private void sendPlain(HttpServletResponse resp, String message) throws IOException {
        sendPlain(resp, message, HttpServletResponse.SC_OK);
    }

    // =====================================================================
    // ====================== BINDING & CONVERSION ========================
    // =====================================================================

    private boolean isCustomObject(Class<?> type) {
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            return !component.isPrimitive()
                    && !component.equals(String.class)
                    && !Number.class.isAssignableFrom(component)
                    && !component.equals(Boolean.class);
        }

        return !type.isPrimitive()
                && !type.equals(String.class)
                && !Number.class.isAssignableFrom(type)
                && !type.equals(Boolean.class)
                && !Map.class.isAssignableFrom(type);
    }

    private Object bindObjectArray(Class<?> arrayType, String paramName,
            Map<String, String[]> params, StringBuilder debug) throws Exception {
        Class<?> componentType = arrayType.getComponentType();
        if (DEBUG)
            debug.append("\n=== Binding tableau ").append(componentType.getSimpleName())
                    .append("[] (prefix: ").append(paramName).append(") ===\n");

        int maxIndex = -1;
        String prefix = paramName + "[";
        for (String key : params.keySet()) {
            if (key.startsWith(prefix)) {
                int end = key.indexOf(']', prefix.length());
                if (end > 0) {
                    try {
                        int idx = Integer.parseInt(key.substring(prefix.length(), end));
                        maxIndex = Math.max(maxIndex, idx);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        if (maxIndex == -1)
            return Array.newInstance(componentType, 0);

        Object array = Array.newInstance(componentType, maxIndex + 1);
        for (int i = 0; i <= maxIndex; i++) {
            String elementPrefix = paramName + "[" + i + "]";
            Object element = bindObject(componentType, elementPrefix, params, debug);
            Array.set(array, i, element);
        }
        return array;
    }

    private Object bindObject(Class<?> targetClass, String prefix,
            Map<String, String[]> params, StringBuilder debug) throws Exception {
        Object instance = targetClass.getDeclaredConstructor().newInstance();

        if (DEBUG)
            debug.append("\n=== Binding objet ").append(targetClass.getSimpleName())
                    .append(" (prefix: ").append(prefix).append(") ===\n");

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix + ".")) {
                String relativePath = key.substring(prefix.length() + 1);
                setNestedValue(instance, relativePath, entry.getValue()[0], debug, 0);
            }
        }
        return instance;
    }

    private void setNestedValue(Object instance, String path, String value,
            StringBuilder debug, int depth) throws Exception {
        String indent = "  ".repeat(depth + 1);

        if (DEBUG)
            debug.append(indent).append("→ ").append(path).append("\n");

        if (!path.contains(".") && !path.contains("[")) {
            setFieldValue(instance, path, value, debug, depth);
            return;
        }

        if (path.contains("[")) {
            int bracketStart = path.indexOf('[');
            String fieldName = path.substring(0, bracketStart);
            int bracketEnd = path.indexOf(']');
            int index = Integer.parseInt(path.substring(bracketStart + 1, bracketEnd));
            String remaining = path.substring(bracketEnd + 1);
            if (remaining.startsWith("."))
                remaining = remaining.substring(1);

            Object element = getOrCreateCollectionElement(instance, fieldName, index, debug, depth);
            if (!remaining.isEmpty()) {
                setNestedValue(element, remaining, value, debug, depth + 1);
            }
            return;
        }

        int dot = path.indexOf('.');
        String fieldName = path.substring(0, dot);
        String remaining = path.substring(dot + 1);

        Field field = findField(instance.getClass(), fieldName);
        if (field == null)
            return;

        field.setAccessible(true);
        Object nested = field.get(instance);
        if (nested == null) {
            nested = field.getType().getDeclaredConstructor().newInstance();
            field.set(instance, nested);
        }
        setNestedValue(nested, remaining, value, debug, depth + 1);
    }

    private Object getOrCreateCollectionElement(Object instance, String fieldName, int index,
            StringBuilder debug, int depth) throws Exception {
        Field field = findField(instance.getClass(), fieldName);
        if (field == null)
            throw new Exception("Field not found: " + fieldName);
        field.setAccessible(true);

        Object collection = field.get(instance);
        Class<?> fieldType = field.getType();

        if (fieldType.isArray()) {
            return handleNativeArray(instance, field, index, fieldType.getComponentType(), debug, depth);
        }

        if (Collection.class.isAssignableFrom(fieldType)) {
            Type generic = field.getGenericType();
            Class<?> elementType = Object.class;
            if (generic instanceof ParameterizedType) {
                Type[] args = ((ParameterizedType) generic).getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?>) {
                    elementType = (Class<?>) args[0];
                }
            }
            return handleGenericCollection(instance, field, fieldType, index, elementType, debug, depth);
        }

        throw new Exception("Unsupported collection type: " + fieldType);
    }

    private Object handleNativeArray(Object instance, Field field, int index,
            Class<?> elementType, StringBuilder debug, int depth) throws Exception {
        Object array = field.get(instance);
        if (array == null || Array.getLength(array) <= index) {
            Object newArray = Array.newInstance(elementType, index + 1);
            if (array != null)
                System.arraycopy(array, 0, newArray, 0, Array.getLength(array));
            field.set(instance, newArray);
            array = newArray;
        }
        Object element = Array.get(array, index);
        if (element == null) {
            element = elementType.getDeclaredConstructor().newInstance();
            Array.set(array, index, element);
        }
        return element;
    }

    @SuppressWarnings("unchecked")
    private Object handleGenericCollection(Object instance, Field field, Class<?> collType,
            int index, Class<?> elementType,
            StringBuilder debug, int depth) throws Exception {
        Collection<Object> coll = (Collection<Object>) field.get(instance);
        if (coll == null) {
            coll = createCollectionInstance(collType);
            field.set(instance, coll);
        }

        List<Object> list = (coll instanceof List) ? (List<Object>) coll : new ArrayList<>(coll);
        while (list.size() <= index)
            list.add(null);
        Object element = list.get(index);
        if (element == null) {
            element = elementType.getDeclaredConstructor().newInstance();
            list.set(index, element);
        }
        if (!(coll instanceof List))
            field.set(instance, list);
        return element;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> createCollectionInstance(Class<?> type) throws Exception {
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            return (Collection<Object>) type.getDeclaredConstructor().newInstance();
        }
        if (List.class.isAssignableFrom(type))
            return new ArrayList<>();
        if (Set.class.isAssignableFrom(type))
            return new HashSet<>();
        return new ArrayList<>();
    }

    private void setFieldValue(Object instance, String fieldName, String value,
            StringBuilder debug, int depth) throws Exception {
        Field field = findField(instance.getClass(), fieldName);
        if (field == null) {
            if (DEBUG)
                debug.append("  ".repeat(depth + 1))
                        .append("Field non trouvé : ").append(fieldName).append("\n");
            return;
        }
        field.setAccessible(true);
        field.set(instance, convertType(value, field.getType()));
    }

    private Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return clazz.getSuperclass() != null ? findField(clazz.getSuperclass(), name) : null;
        }
    }

    private Object convertType(String value, Class<?> targetType) {
        if (value == null || value.isBlank())
            return null;
        if (targetType == String.class)
            return value;
        if (targetType == int.class || targetType == Integer.class)
            return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class)
            return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class)
            return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class)
            return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(value);
        return value;
    }
}