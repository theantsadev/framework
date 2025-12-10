package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import servlet.annotations.RequestParam;
import servlet.utils.MethodInvoker;
import servlet.utils.RouteMatch;
import servlet.utils.UrlRouter;

public class FrontServlet extends HttpServlet {

    private static final boolean DEBUG = true;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();
        UrlRouter routes = (UrlRouter) context.getAttribute("routes");

        String httpMethod = req.getMethod();
        String path = req.getRequestURI().substring(req.getContextPath().length());
        RouteMatch routeMatch = (routes != null) ? routes.findByUrl(path, httpMethod) : null;

        StringBuilder debugOutput = new StringBuilder();

        if (DEBUG) {
            debugOutput.append("\n========== DEBUG FrontServlet ==========\n");
            debugOutput.append("Routes totales    : ").append(routes != null ? routes.size() : 0).append("\n");
            debugOutput.append("HTTP Method       : ").append(httpMethod).append("\n");
            debugOutput.append("URL appelée       : ").append(path).append("\n");
            System.out.println(debugOutput.toString());
        }

        if (routeMatch == null) {
            resp.setContentType("text/plain");
            if (DEBUG) {
                resp.getWriter().print(debugOutput.toString());
                resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
            } else {
                resp.getWriter().print("404 - Aucun contrôleur trouvé pour " + path);
            }
            return;
        }

        MethodInvoker invoker = routeMatch.getMethodInvoker();
        Map<String, String> pathParams = routeMatch.getPathParams();
        Map<String, String[]> queryParams = req.getParameterMap();

        if (DEBUG) {
            debugOutput.append("Route trouvée     : ").append(invoker.getMethod().getName()).append("\n");
            debugOutput.append("Paramètres PATH   : ").append(pathParams).append("\n");
            debugOutput.append("Paramètres QUERY  : ").append(queryParams).append("\n");
            System.out.println("Route trouvée     : " + invoker.getMethod().getName());
            System.out.println("Paramètres PATH   : " + pathParams);
            System.out.println("Paramètres QUERY  : " + queryParams);
        }

        try {
            Method method = invoker.getMethod();
            Parameter[] methodParameters = method.getParameters();
            Object[] args = new Object[methodParameters.length];

            if (DEBUG) {
                debugOutput.append("\n-- Méthode cible --\n");
                debugOutput.append("Classe : ").append(method.getDeclaringClass().getName()).append("\n");
                debugOutput.append("Méthode : ").append(method.getName()).append("\n");
                System.out.println("\n-- Méthode cible --");
                System.out.println("Classe : " + method.getDeclaringClass().getName());
                System.out.println("Méthode : " + method.getName());
            }

            // Injection des paramètres
            for (int i = 0; i < methodParameters.length; i++) {
                Parameter param = methodParameters[i];
                String paramName = null;

                // 1. Vérifier annotation RequestParam
                if (param.isAnnotationPresent(RequestParam.class)) {
                    RequestParam reqParam = param.getAnnotation(RequestParam.class);
                    paramName = reqParam.name();
                }

                // 2. Si annotation vide ou absente → utiliser nom réel
                if (paramName == null || paramName.isEmpty()) {
                    paramName = param.getName();
                }

                // 3. Injection Map<String,Object> si paramètre Map
                if (Map.class.isAssignableFrom(param.getType())) {
                    args[i] = queryParams.entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue()[0]));
                    continue;
                }

                // 4. Si c'est un objet custom (pas un type simple), essayer le binding
                if (isCustomObject(param.getType())) {
                    // Vérifier si c'est un tableau d'objets (ex: Employee[] es)
                    if (param.getType().isArray()) {
                        args[i] = bindObjectArray(param.getType(), paramName, queryParams, debugOutput);

                        if (DEBUG) {
                            debugOutput.append("\nParamètre #").append(i).append(" (Array Object Binding)\n");
                            debugOutput.append("  Nom Java        : ").append(param.getName()).append("\n");
                            debugOutput.append("  Type            : ").append(param.getType().getSimpleName())
                                    .append("\n");
                            debugOutput.append("  Tableau créé    : ").append(args[i]).append("\n");

                            System.out.println("\nParamètre #" + i + " (Array Object Binding)");
                            System.out.println("  Nom Java        : " + param.getName());
                            System.out.println("  Type            : " + param.getType().getSimpleName());
                            System.out.println("  Tableau créé    : " + args[i]);
                        }
                    } else {
                        // Objet simple
                        args[i] = bindObject(param.getType(), paramName, queryParams, debugOutput);

                        if (DEBUG) {
                            debugOutput.append("\nParamètre #").append(i).append(" (Object Binding)\n");
                            debugOutput.append("  Nom Java        : ").append(param.getName()).append("\n");
                            debugOutput.append("  Type            : ").append(param.getType().getSimpleName())
                                    .append("\n");
                            debugOutput.append("  Objet créé      : ").append(args[i]).append("\n");

                            System.out.println("\nParamètre #" + i + " (Object Binding)");
                            System.out.println("  Nom Java        : " + param.getName());
                            System.out.println("  Type            : " + param.getType().getSimpleName());
                            System.out.println("  Objet créé      : " + args[i]);
                        }
                    }
                    continue;
                }

                // 5. Récupérer valeur : priorité PATH, sinon QUERY
                String raw = null;
                if (pathParams != null && pathParams.containsKey(paramName)) {
                    raw = pathParams.get(paramName);
                } else if (queryParams != null && queryParams.containsKey(paramName)) {
                    String[] rawValues = queryParams.get(paramName);
                    raw = (rawValues != null && rawValues.length > 0) ? rawValues[0] : null;
                }

                // 6. Conversion automatique
                Object converted = convertType(raw, param.getType());
                args[i] = converted;

                if (DEBUG) {
                    debugOutput.append("\nParamètre #").append(i).append("\n");
                    debugOutput.append("  Nom Java        : ").append(param.getName()).append("\n");
                    debugOutput.append("  Nom attendu     : ").append(paramName).append("\n");
                    debugOutput.append("  Type            : ").append(param.getType().getSimpleName()).append("\n");
                    debugOutput.append("  Valeur brute    : ").append(raw).append("\n");
                    debugOutput.append("  Valeur convertie: ").append(converted).append("\n");

                    System.out.println("\nParamètre #" + i);
                    System.out.println("  Nom Java        : " + param.getName());
                    System.out.println("  Nom attendu     : " + paramName);
                    System.out.println("  Type            : " + param.getType().getSimpleName());
                    System.out.println("  Valeur brute    : " + raw);
                    System.out.println("  Valeur convertie: " + converted);
                }
            }

            // 7. Exécution de la méthode
            Object result = invoker.execute(args);

            if (DEBUG) {
                debugOutput.append("\n-- Retour méthode --\n");
                debugOutput.append("Valeur retournée: ").append(result).append("\n");
                debugOutput.append("Type de retour  : ")
                        .append(result != null ? result.getClass().getSimpleName() : "null").append("\n");
                debugOutput.append("=======================================\n");

                System.out.println("\n-- Retour méthode --");
                System.out.println("Valeur retournée: " + result);
                System.out
                        .println("Type de retour  : " + (result != null ? result.getClass().getSimpleName() : "null"));
                System.out.println("=======================================\n");
            }

            // 8. Traitement du retour
            if (result instanceof String) {
                resp.setContentType("text/plain;charset=UTF-8");
                PrintWriter writer = resp.getWriter();

                if (DEBUG) {
                    writer.println(debugOutput.toString());
                    writer.println("\n========== RÉSULTAT ==========");
                }

                writer.println(result);
                return;
            }

            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                String view = mv.getView();
                String viewPath = "/" + view;
                File jspFile = new File(context.getRealPath(viewPath));

                if (jspFile.exists() && jspFile.isFile()) {
                    if (DEBUG) {
                        req.setAttribute("_debugOutput", debugOutput.toString());
                    }
                    mv.passVar(req);
                    RequestDispatcher rd = context.getRequestDispatcher(viewPath);
                    rd.forward(req, resp);
                } else {
                    resp.setContentType("text/plain");
                    if (DEBUG) {
                        resp.getWriter().println(debugOutput.toString());
                        resp.getWriter().println("\n ERREUR : Vue introuvable : " + context.getRealPath(viewPath));
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                "Vue introuvable : " + context.getRealPath(viewPath));
                    }
                }
                return;
            }

            // Fallback si type de retour inconnu
            resp.setContentType("text/plain");
            PrintWriter writer = resp.getWriter();

            if (DEBUG) {
                writer.println(debugOutput.toString());
                writer.println("\n========== RÉSULTAT (type inconnu) ==========");
            }

            writer.println(result);

        } catch (Exception e) {
            resp.setContentType("text/plain");
            PrintWriter writer = resp.getWriter();

            if (DEBUG) {
                writer.println(debugOutput.toString());
                writer.println("\n==========  ERREUR ==========");
            }

            e.printStackTrace(writer);
        }
    }

    /**
     * Vérifie si le type est un objet custom (pas un type primitif/wrapper/String)
     */
    private boolean isCustomObject(Class<?> type) {
        // Si c'est un tableau, vérifier le type des éléments
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            return !componentType.isPrimitive()
                    && !componentType.equals(String.class)
                    && !componentType.equals(Integer.class)
                    && !componentType.equals(Double.class)
                    && !componentType.equals(Boolean.class)
                    && !componentType.equals(Long.class)
                    && !componentType.equals(Float.class);
        }

        return !type.isPrimitive()
                && !type.equals(String.class)
                && !type.equals(Integer.class)
                && !type.equals(Double.class)
                && !type.equals(Boolean.class)
                && !type.equals(Long.class)
                && !type.equals(Float.class)
                && !Map.class.isAssignableFrom(type);
    }

    /**
     * Crée et remplit un tableau d'objets à partir des paramètres du formulaire
     * Ex: Employee[] es avec e[0].name, e[1].name, etc.
     */
    private Object bindObjectArray(Class<?> arrayType, String paramName, Map<String, String[]> params,
            StringBuilder debug) throws Exception {
        Class<?> componentType = arrayType.getComponentType();

        if (DEBUG) {
            debug.append("\n=== Binding tableau d'objets ").append(componentType.getSimpleName())
                    .append("[] (param: ").append(paramName).append(") ===\n");
        }

        // Trouver le nombre max d'éléments en scannant les paramètres
        int maxIndex = -1;
        String prefix = paramName + "[";

        for (String key : params.keySet()) {
            if (key.startsWith(prefix)) {
                int bracketEnd = key.indexOf(']', prefix.length());
                if (bracketEnd > 0) {
                    try {
                        int index = Integer.parseInt(key.substring(prefix.length(), bracketEnd));
                        maxIndex = Math.max(maxIndex, index);
                    } catch (NumberFormatException e) {
                        // Ignorer si ce n'est pas un nombre
                    }
                }
            }
        }

        if (maxIndex < 0) {
            if (DEBUG) {
                debug.append("  Aucun élément trouvé, retour tableau vide\n");
            }
            return Array.newInstance(componentType, 0);
        }

        // Créer le tableau
        Object arrayObj = Array.newInstance(componentType, maxIndex + 1);

        if (DEBUG) {
            debug.append("  Création tableau de taille ").append(maxIndex + 1).append("\n");
        }

        // Remplir chaque élément
        for (int i = 0; i <= maxIndex; i++) {
            String elementPrefix = paramName + "[" + i + "]";
            Object element = bindObject(componentType, elementPrefix, params, debug);
            Array.set(arrayObj, i, element);

            if (DEBUG) {
                debug.append("  Élément [").append(i).append("] créé: ").append(element).append("\n");
            }
        }

        return arrayObj;
    }

    /**
     * Crée et remplit un objet à partir des paramètres du formulaire
     * Gère la notation pointée nestée : employe.departements[0].regions[1].nom
     * Supporte aussi List, ArrayList, Vector, etc. grâce à la réflexion
     */
    private Object bindObject(Class<?> targetClass, String prefix, Map<String, String[]> params, StringBuilder debug)
            throws Exception {
        Object instance = targetClass.getDeclaredConstructor().newInstance();

        if (DEBUG) {
            debug.append("\n=== Binding objet ").append(targetClass.getSimpleName()).append(" (prefix: ").append(prefix)
                    .append(") ===\n");
        }

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String fullKey = entry.getKey();
            String value = entry.getValue()[0];

            // Vérifier si le paramètre commence par le préfixe attendu (ex: "employe.")
            if (fullKey.startsWith(prefix + ".")) {
                String relativePath = fullKey.substring(prefix.length() + 1); // Enlever "employe."

                if (DEBUG) {
                    debug.append("  Traitement : ").append(fullKey).append(" = ").append(value).append("\n");
                }

                // Traiter le chemin de manière récursive
                setNestedValue(instance, relativePath, value, debug, 0);
            }
        }

        return instance;
    }

    /**
     * Définit une valeur dans un chemin potentiellement nesté de manière récursive
     * Supporte : nom, departements[0].nom, departements[0].regions[1].nom, etc.
     * AMÉLIORATION: Détecte automatiquement si c'est un tableau, List, Vector, etc.
     */
    private void setNestedValue(Object instance, String path, String value, StringBuilder debug, int depth)
            throws Exception {
        String indent = "  ".repeat(depth + 1);

        if (DEBUG) {
            debug.append(indent).append("→ setNestedValue sur ")
                    .append(instance.getClass().getSimpleName())
                    .append(", path='").append(path).append("'\n");
        }

        // Cas 1: Propriété simple (pas de . ni de [)
        if (!path.contains(".") && !path.contains("[")) {
            setFieldValue(instance, path, value, debug, depth);
            return;
        }

        // Cas 2: Collection/Tableau suivi de plus de nesting (ex:
        // departements[0].regions[1].nom)
        if (path.contains("[")) {
            int bracketStart = path.indexOf('[');
            String collectionFieldName = path.substring(0, bracketStart); // "departements"

            // Extraire l'index et le reste du chemin
            int bracketEnd = path.indexOf(']');
            int index = Integer.parseInt(path.substring(bracketStart + 1, bracketEnd)); // 0

            String remainingPath = path.substring(bracketEnd + 1); // ".regions[1].nom" ou ".nom"
            if (remainingPath.startsWith(".")) {
                remainingPath = remainingPath.substring(1); // "regions[1].nom" ou "nom"
            }

            if (DEBUG) {
                debug.append(indent).append("  Collection: field='").append(collectionFieldName)
                        .append("', index=").append(index)
                        .append(", remaining='").append(remainingPath).append("'\n");
            }

            // Gérer la collection (tableau, List, Vector, etc.) et l'élément à l'index
            Object element = getOrCreateCollectionElement(instance, collectionFieldName, index, debug, depth);

            // Récursion sur le reste du chemin
            if (!remainingPath.isEmpty()) {
                setNestedValue(element, remainingPath, value, debug, depth + 1);
            }
            return;
        }

        // Cas 3: Propriété suivie de plus de nesting (ex: address.city)
        int dotIndex = path.indexOf('.');
        String currentField = path.substring(0, dotIndex); // "address"
        String remainingPath = path.substring(dotIndex + 1); // "city" ou "regions[0].nom"

        if (DEBUG) {
            debug.append(indent).append("  Nested: field='").append(currentField)
                    .append("', remaining='").append(remainingPath).append("'\n");
        }

        // Récupérer ou créer l'objet nested
        Field field = findField(instance.getClass(), currentField);
        if (field == null) {
            if (DEBUG) {
                debug.append(indent).append("  ERREUR: Field '").append(currentField).append("' non trouvé\n");
            }
            return;
        }

        field.setAccessible(true);
        Object nestedObject = field.get(instance);

        // Créer l'objet s'il n'existe pas
        if (nestedObject == null) {
            nestedObject = field.getType().getDeclaredConstructor().newInstance();
            field.set(instance, nestedObject);

            if (DEBUG) {
                debug.append(indent).append("  Création objet nested: ").append(field.getType().getSimpleName())
                        .append("\n");
            }
        }

        // Récursion sur le reste du chemin
        setNestedValue(nestedObject, remainingPath, value, debug, depth + 1);
    }

    /**
     * Récupère ou crée un élément dans une collection (tableau, List, Vector, etc.)
     * à l'index donné
     * UTILISE LA RÉFLEXION pour détecter le type de collection et le type d'élément
     */
    private Object getOrCreateCollectionElement(Object instance, String fieldName, int index, StringBuilder debug,
            int depth) throws Exception {
        String indent = "  ".repeat(depth + 1);

        // Récupérer le field de la collection
        Field collectionField = findField(instance.getClass(), fieldName);
        if (collectionField == null) {
            if (DEBUG) {
                debug.append(indent).append("  ERREUR: Collection field '").append(fieldName).append("' non trouvé\n");
            }
            throw new Exception("Collection field '" + fieldName + "' not found");
        }

        collectionField.setAccessible(true);
        Class<?> fieldType = collectionField.getType();

        // Déterminer le type d'élément via réflexion
        Class<?> elementType = null;

        // CAS 1: Tableau natif (Type[])
        if (fieldType.isArray()) {
            elementType = fieldType.getComponentType();

            if (DEBUG) {
                debug.append(indent).append("  Type détecté: Tableau natif (")
                        .append(elementType.getSimpleName()).append("[])\n");
            }

            return handleNativeArray(instance, collectionField, index, elementType, debug, depth);
        }

        // CAS 2: Collection générique (List<T>, Vector<T>, ArrayList<T>, etc.)
        if (Collection.class.isAssignableFrom(fieldType)) {
            // Extraire le type générique T de List<T>, Vector<T>, etc.
            Type genericType = collectionField.getGenericType();

            if (genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    elementType = (Class<?>) typeArgs[0];

                    if (DEBUG) {
                        debug.append(indent).append("  Type détecté: Collection générique ")
                                .append(fieldType.getSimpleName()).append("<")
                                .append(elementType.getSimpleName()).append(">\n");
                    }

                    return handleGenericCollection(instance, collectionField, fieldType, index, elementType, debug,
                            depth);
                }
            }

            // Si pas de type générique détectable, erreur
            if (DEBUG) {
                debug.append(indent).append("  ERREUR: Impossible de détecter le type générique\n");
            }
            throw new Exception("Cannot detect generic type for collection field '" + fieldName + "'");
        }

        // Type non supporté
        if (DEBUG) {
            debug.append(indent).append("  ERREUR: Type de collection non supporté: ")
                    .append(fieldType.getSimpleName()).append("\n");
        }
        throw new Exception("Unsupported collection type: " + fieldType.getName());
    }

    /**
     * Gère un tableau natif (Type[])
     */
    private Object handleNativeArray(Object instance, Field arrayField, int index, Class<?> elementType,
            StringBuilder debug, int depth) throws Exception {
        String indent = "  ".repeat(depth + 1);

        arrayField.setAccessible(true);
        Object arrayObj = arrayField.get(instance);

        // Si le tableau n'existe pas encore, le créer
        if (arrayObj == null) {
            arrayObj = Array.newInstance(elementType, index + 1);
            arrayField.set(instance, arrayObj);

            if (DEBUG) {
                debug.append(indent).append("  Création tableau de type ")
                        .append(elementType.getSimpleName())
                        .append("[").append(index + 1).append("]\n");
            }
        }

        // Si le tableau est trop petit, l'agrandir
        int currentLength = Array.getLength(arrayObj);
        if (index >= currentLength) {
            Object newArray = Array.newInstance(elementType, index + 1);
            System.arraycopy(arrayObj, 0, newArray, 0, currentLength);
            arrayObj = newArray;
            arrayField.set(instance, arrayObj);

            if (DEBUG) {
                debug.append(indent).append("  Agrandissement tableau à ").append(index + 1).append("\n");
            }
        }

        // Récupérer ou créer l'élément à l'index
        Object element = Array.get(arrayObj, index);
        if (element == null) {
            element = elementType.getDeclaredConstructor().newInstance();
            Array.set(arrayObj, index, element);

            if (DEBUG) {
                debug.append(indent).append("  Création élément ")
                        .append(elementType.getSimpleName())
                        .append(" à index ").append(index).append("\n");
            }
        }

        return element;
    }

    /**
     * Gère une collection générique (List<T>, Vector<T>, ArrayList<T>, etc.)
     */
    @SuppressWarnings("unchecked")
    private Object handleGenericCollection(Object instance, Field collectionField, Class<?> collectionType,
            int index, Class<?> elementType, StringBuilder debug, int depth) throws Exception {
        String indent = "  ".repeat(depth + 1);

        collectionField.setAccessible(true);
        Collection<Object> collection = (Collection<Object>) collectionField.get(instance);

        // Si la collection n'existe pas, la créer
        if (collection == null) {
            collection = createCollectionInstance(collectionType);
            collectionField.set(instance, collection);

            if (DEBUG) {
                debug.append(indent).append("  Création collection ")
                        .append(collection.getClass().getSimpleName()).append("\n");
            }
        }

        // Convertir en List pour accès par index
        List<Object> list;
        if (collection instanceof List) {
            list = (List<Object>) collection;
        } else {
            // Si ce n'est pas une List, convertir (Vector, etc.)
            list = new ArrayList<>(collection);
            collectionField.set(instance, list);

            if (DEBUG) {
                debug.append(indent).append("  Conversion en ArrayList pour accès par index\n");
            }
        }

        // Agrandir la liste si nécessaire avec des null
        while (list.size() <= index) {
            list.add(null);
        }

        // Récupérer ou créer l'élément à l'index
        Object element = list.get(index);
        if (element == null) {
            element = elementType.getDeclaredConstructor().newInstance();
            list.set(index, element);

            if (DEBUG) {
                debug.append(indent).append("  Création élément ")
                        .append(elementType.getSimpleName())
                        .append(" à index ").append(index).append("\n");
            }
        }

        return element;
    }

    /**
     * Crée une instance de collection selon le type (List → ArrayList, Vector →
     * Vector, etc.)
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> createCollectionInstance(Class<?> collectionType) throws Exception {
        // Si c'est une classe concrète, l'instancier directement
        if (!collectionType.isInterface() && !Modifier.isAbstract(collectionType.getModifiers())) {
            return (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
        }

        // Si c'est une interface, choisir l'implémentation par défaut
        if (List.class.isAssignableFrom(collectionType)) {
            return new ArrayList<>();
        }
        if (Set.class.isAssignableFrom(collectionType)) {
            return new HashSet<>();
        }

        // Fallback: ArrayList
        return new ArrayList<>();
    }

    /**
     * Définit la valeur d'un field d'un objet (propriété simple, feuille de
     * l'arbre)
     */
    private void setFieldValue(Object instance, String fieldName, String value, StringBuilder debug, int depth)
            throws Exception {
        String indent = "  ".repeat(depth + 1);

        Field field = findField(instance.getClass(), fieldName);

        if (field == null) {
            if (DEBUG) {
                debug.append(indent).append("  ATTENTION: Field '").append(fieldName)
                        .append("' non trouvé dans ").append(instance.getClass().getSimpleName()).append("\n");
            }
            return;
        }

        field.setAccessible(true);
        Object convertedValue = convertType(value, field.getType());
        field.set(instance, convertedValue);

        if (DEBUG) {
            debug.append(indent).append("  ✓ Set ").append(instance.getClass().getSimpleName())
                    .append(".").append(fieldName).append(" = ").append(convertedValue).append("\n");
        }
    }

    /**
     * Trouve un field dans une classe (case-sensitive)
     */
    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Essayer avec les fields de la classe parente
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }

    /**
     * Conversion automatique pour types simples
     */
    private Object convertType(String value, Class<?> type) {
        if (value == null || value.trim().isEmpty())
            return null;
        if (type == String.class)
            return value;
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(value);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(value);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(value);
        if (type == long.class || type == Long.class)
            return Long.parseLong(value);
        if (type == float.class || type == Float.class)
            return Float.parseFloat(value);
        return value;
    }
}