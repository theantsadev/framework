package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import servlet.annotations.*;

public class FrontServlet extends HttpServlet {

    private Map<String, Method> routes = new HashMap<>();

    // init est executé une seule fois au lancement de ce servlet
    @Override
    public void init() throws ServletException {
        try {
            // On teste de parcourir les methodes de la classe HelloController
            Class<?> clazz = Class.forName("com.itu.gest_emp.controller.HelloController");
            for (Method m : clazz.getDeclaredMethods()) {
                // On teste si les méthodes sont surmontés d'une annotation @Url ou non
                if (m.isAnnotationPresent(Url.class)) {
                    // Si oui , on recupere l'url en le stockant dans un map (valeur,methode)
                    Url ann = m.getAnnotation(Url.class);
                    routes.put(ann.value(), m);
                }
            }
            // donc on utilise la reflexion pour la seule raison de parcourir toutes les
            // méthodes d'une classe ici
            // car notre objectif ici est d'appeller une méthode d'une classe via url
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    // service est equivalent à doGet + doPost qui prend en charge les requetes et
    // les reponses
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // pour recupérer la requete
        String path = req.getPathInfo();

        // pour avoir la méthode correspondante à cette requete (url)
        Method action = routes.get(path);

        // pour tester si la méthode existe ou non (ressource)
        if (action != null) {
            try {

                // Tentative de recuperation du resultat de la méthode = valeur de retour de la
                // fonction
                Object controller = action.getDeclaringClass().getDeclaredConstructor().newInstance();
                Object result = action.invoke(controller);

                // On essaie d'afficher le résultat
                resp.setContentType("text/plain");
                resp.getWriter().print(result);

            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {

            // Sinon on affiche l'url
            resp.setContentType("text/plain");
            resp.getWriter().print(path);

        }
    }

}