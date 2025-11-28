package servlet.listener;

import java.util.List;

import servlet.annotations.Controller;
import servlet.annotations.Url;
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
            for (Class<?> c : classes) {
                if (c.isAnnotationPresent(Controller.class)) {
                    String baseUrl = "";
                    if (c.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping requestMapping = c.getAnnotation(RequestMapping.class);
                        baseUrl = requestMapping.value();
                    }
                    for (var m : c.getDeclaredMethods()) {
                        if (m.isAnnotationPresent(Url.class)) {
                            Url annotation = m.getAnnotation(Url.class);
                            String value = annotation.value();
                            routes.put(baseUrl + value, new MethodInvoker(c, m));
                        }
                    }

                }
            }

            context.setAttribute("routes", routes);
            System.out.println("✅ Routes enregistrées (" + routes.size() + ") dans ServletContext");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
