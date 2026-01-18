package servlet.listener;

import java.util.List;

import servlet.annotations.Controller;
import servlet.annotations.mapping.RequestMapping;
import servlet.utils.ClassDetector;
import servlet.utils.MethodInvoker;
import servlet.utils.UrlRouter;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class RouteInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();
            UrlRouter routes = new UrlRouter();

            List<Class<?>> classes = ClassDetector.getAllClassesFromClasspath();
            

            for (Class<?> controllerClass : classes) {

                if (controllerClass.isAnnotationPresent(Controller.class)) {

                    for (var method : controllerClass.getDeclaredMethods()) {

                        RequestMapping mapping = getRequestMapping(method);

                        if (mapping != null) {
                            String urlPattern = mapping.value();
                            String httpMethod = mapping.method().name();

                            MethodInvoker invoker = new MethodInvoker(controllerClass, method);
                            routes.addRoute(urlPattern, httpMethod, invoker);

                            System.out.println(" Route ajoutée : [" + httpMethod + "] " + urlPattern
                                    + "  → " + controllerClass.getSimpleName() + "." + method.getName());
                        }
                    }
                }
            }

            context.setAttribute("routes", routes);
            System.out.println("✅ Routes enregistrées (" + routes.size() + " URLs uniques)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Méthode helper pour récupérer @RequestMapping (direct ou via
    // méta-annotation)
    private RequestMapping getRequestMapping(java.lang.reflect.Method method) {
        // 1 Vérifier si @RequestMapping est directement présent
        if (method.isAnnotationPresent(RequestMapping.class)) {
            return method.getAnnotation(RequestMapping.class);
        }

        // 2 Chercher dans les méta-annotations (@GetMapping, @PostMapping, etc.)
        for (var annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(RequestMapping.class)) {
                // Récupérer le @RequestMapping de l'annotation (@GetMapping → @RequestMapping)
                RequestMapping metaMapping = annotation.annotationType()
                        .getAnnotation(RequestMapping.class);

                // Récupérer la valeur (URL) de l'annotation concrète (@GetMapping(value="..."))
                try {
                    java.lang.reflect.Method valueMethod = annotation.annotationType()
                            .getDeclaredMethod("value");
                    String url = (String) valueMethod.invoke(annotation);

                    // Créer un proxy pour fusionner les deux
                    return new RequestMapping() {
                        @Override
                        public Class<? extends java.lang.annotation.Annotation> annotationType() {
                            return RequestMapping.class;
                        }

                        @Override
                        public String value() {
                            return url.isEmpty() ? metaMapping.value() : url;
                        }

                        @Override
                        public servlet.enums.RequestMethod method() {
                            return metaMapping.method();
                        }
                    };

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
