package servlet.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indique qu'une méthode nécessite un rôle spécifique.
 * L'utilisateur doit être authentifié ET avoir le rôle requis.
 * Le nom de la variable de rôle est configuré dans security.properties
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {
    /**
     * Le rôle requis pour accéder à cette méthode
     */
    String value();
}
