package servlet.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour injecter automatiquement la session dans les paramètres du contrôleur
 * 
 * Utilisation:
 * @PostMapping("/login")
 * public String login(@Session CustomSession session, String username) {
 *     session.setAttribute("user", username);
 *     return "success";
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Session {
    /**
     * Crée une nouvelle session si elle n'existe pas
     */
    boolean create() default true;
}