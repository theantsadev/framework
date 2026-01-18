package servlet.binding;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import servlet.annotations.RequestParam;
import servlet.utils.RouteMatch;
import servlet.utils.Upload;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ArgumentResolver {

    private static final boolean DEBUG = true;
    private final TypeConverter converter = new TypeConverter();

    public Object[] resolve(RouteMatch routeMatch, HttpServletRequest req, StringBuilder debug) throws Exception {
        Method method = routeMatch.getMethodInvoker().getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, String> pathParams = routeMatch.getPathParams();
        Map<String, String[]> queryParams = req.getParameterMap();

        // Vérifier si c'est une requête multipart (upload)
        boolean isMultipart = req.getContentType() != null &&
                req.getContentType().toLowerCase().startsWith("multipart/form-data");

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = getParameterName(param);
            Class<?> paramType = param.getType();

            // Gérer Map<String, List<Upload>> pour les uploads multiples
            if (isMultipart && Map.class.isAssignableFrom(paramType)) {
                Type genericType = param.getParameterizedType();
                if (genericType instanceof ParameterizedType) {
                    Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (typeArgs.length == 2 &&
                            typeArgs[0].equals(String.class) &&
                            typeArgs[1] instanceof ParameterizedType) {
                        ParameterizedType listType = (ParameterizedType) typeArgs[1];
                        if (listType.getRawType().equals(List.class) &&
                                listType.getActualTypeArguments()[0].equals(Upload.class)) {
                            args[i] = collectAllUploads(req, debug);
                            continue;
                        }
                    }
                }
                // Sinon, comportement par défaut (Map des paramètres)
                args[i] = queryParams.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
                continue;
            }

            // Gérer List<Upload> pour un champ spécifique
            if (isMultipart && List.class.isAssignableFrom(paramType)) {
                Type genericType = param.getParameterizedType();
                if (genericType instanceof ParameterizedType) {
                    Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                    if (typeArgs.length == 1 && typeArgs[0].equals(Upload.class)) {
                        args[i] = getUploadsByName(req, paramName, debug);
                        continue;
                    }
                }
            }

            // Gérer Upload unique
            if (isMultipart && Upload.class.equals(paramType)) {
                args[i] = getSingleUpload(req, paramName, debug);
                continue;
            }

            // Comportement normal pour les autres types
            if (Map.class.isAssignableFrom(paramType)) {
                args[i] = queryParams.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
                continue;
            }

            if (isCustomObject(paramType)) {
                if (paramType.isArray()) {
                    args[i] = bindObjectArray(paramType, paramName, queryParams, debug);
                } else {
                    args[i] = bindObject(paramType, paramName, queryParams, debug);
                }
                continue;
            }

            String rawValue = pathParams != null && pathParams.containsKey(paramName)
                    ? pathParams.get(paramName)
                    : (queryParams.containsKey(paramName) ? queryParams.get(paramName)[0] : null);

            args[i] = converter.convert(rawValue, paramType);

            if (DEBUG && debug != null) {
                debug.append("Param #").append(i)
                        .append(" [").append(paramName)
                        .append("]  ").append(rawValue)
                        .append("  →  ").append(args[i])
                        .append(" (").append(paramType.getSimpleName()).append(")\n");
            }
        }
        return args;
    }

    private Map<String, List<Upload>> collectAllUploads(HttpServletRequest req, StringBuilder debug) throws Exception {
        Map<String, List<Upload>> uploadMap = new HashMap<>();
        String uploadDir = req.getServletContext().getRealPath("/uploads");

        if (DEBUG && debug != null) {
            debug.append("\n=== Collecte des uploads ===\n");
            debug.append("Upload directory: ").append(uploadDir).append("\n");
        }

        for (Part part : req.getParts()) {
            String fileName = part.getSubmittedFileName();
            if (fileName != null && !fileName.isEmpty()) {
                String fieldName = part.getName();
                Upload upload = new Upload(part, uploadDir);

                uploadMap.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(upload);

                if (DEBUG && debug != null) {
                    debug.append("  ").append(fieldName).append(": ")
                            .append(fileName).append(" (")
                            .append(part.getSize()).append(" bytes)\n");
                }
            }
        }

        return uploadMap;
    }

    private List<Upload> getUploadsByName(HttpServletRequest req, String fieldName, StringBuilder debug)
            throws Exception {
        List<Upload> uploads = new ArrayList<>();
        String uploadDir = req.getServletContext().getRealPath("/uploads");

        for (Part part : req.getParts()) {
            if (part.getName().equals(fieldName)) {
                String fileName = part.getSubmittedFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    Upload upload = new Upload(part, uploadDir);
                    uploads.add(upload);

                    if (DEBUG && debug != null) {
                        debug.append("Upload: ").append(fieldName).append(" → ")
                                .append(fileName).append(" (")
                                .append(part.getSize()).append(" bytes)\n");
                    }
                }
            }
        }

        return uploads;
    }

    private Upload getSingleUpload(HttpServletRequest req, String fieldName, StringBuilder debug) throws Exception {
        String uploadDir = req.getServletContext().getRealPath("/uploads");

        for (Part part : req.getParts()) {
            if (part.getName().equals(fieldName)) {
                String fileName = part.getSubmittedFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    Upload upload = new Upload(part, uploadDir);

                    if (DEBUG && debug != null) {
                        debug.append("Upload: ").append(fieldName).append(" → ")
                                .append(fileName).append(" (")
                                .append(part.getSize()).append(" bytes)\n");
                    }

                    return upload;
                }
            }
        }

        return null;
    }

    private String getParameterName(Parameter param) {
        RequestParam annotation = param.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return param.getName();
    }

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

    // ===================== BINDING OBJETS & TABLEAUX =====================

    private Object bindObjectArray(Class<?> arrayType, String paramName,
            Map<String, String[]> params, StringBuilder debug) throws Exception {
        Class<?> componentType = arrayType.getComponentType();
        if (DEBUG && debug != null) {
            debug.append("\n=== Binding tableau ").append(componentType.getSimpleName())
                    .append("[] (prefix: ").append(paramName).append(") ===\n");
        }

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

        if (DEBUG && debug != null) {
            debug.append("\n=== Binding objet ").append(targetClass.getSimpleName())
                    .append(" (prefix: ").append(prefix).append(") ===\n");
        }

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
        if (DEBUG && debug != null)
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
            if (DEBUG && debug != null) {
                debug.append("  ".repeat(depth + 1)).append("Field non trouvé : ").append(fieldName).append("\n");
            }
            return;
        }
        field.setAccessible(true);
        field.set(instance, converter.convert(value, field.getType()));
    }

    private Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return clazz.getSuperclass() != null ? findField(clazz.getSuperclass(), name) : null;
        }
    }
}