package servlet.security;

import java.lang.reflect.Method;
import servlet.annotations.Authorized;
import servlet.annotations.Role;
import servlet.session.CustomSession;

/**
 * Vérifie les autorisations (@Authorized et @Role) avant l'exécution d'une méthode
 */
public class SecurityChecker {
    
    private final SecurityConfig config;
    
    public SecurityChecker() {
        this.config = SecurityConfig.getInstance();
    }
    
    /**
     * Vérifie si l'utilisateur est autorisé à exécuter la méthode
     * 
     * @param method La méthode à exécuter
     * @param session La session utilisateur (peut être null)
     * @throws UnauthorizedException si l'utilisateur n'est pas authentifié
     * @throws ForbiddenException si l'utilisateur n'a pas le rôle requis
     */
    public void checkAuthorization(Method method, CustomSession session) 
            throws UnauthorizedException, ForbiddenException {
        
        boolean requiresAuth = method.isAnnotationPresent(Authorized.class);
        Role roleAnnotation = method.getAnnotation(Role.class);
        
        // Si @Role est présent, l'authentification est implicitement requise
        if (roleAnnotation != null) {
            requiresAuth = true;
        }
        
        // Vérifier l'authentification
        if (requiresAuth) {
            if (!isAuthenticated(session)) {
                throw new UnauthorizedException();
            }
        }
        
        // Vérifier le rôle
        if (roleAnnotation != null) {
            String requiredRole = roleAnnotation.value();
            if (!hasRole(session, requiredRole)) {
                throw new ForbiddenException(requiredRole);
            }
        }
    }
    
    /**
     * Vérifie si l'utilisateur est authentifié
     */
    private boolean isAuthenticated(CustomSession session) {
        if (session == null) {
            return false;
        }
        
        String authVar = config.getAuthSessionVariable();
        Object authValue = session.getAttribute(authVar);
        
        // L'utilisateur est authentifié si la variable existe et n'est pas null/false
        if (authValue == null) {
            return false;
        }
        
        if (authValue instanceof Boolean) {
            return (Boolean) authValue;
        }
        
        // Si c'est un objet (ex: User), considérer comme authentifié
        return true;
    }
    
    /**
     * Vérifie si l'utilisateur a le rôle requis
     */
    private boolean hasRole(CustomSession session, String requiredRole) {
        if (session == null || requiredRole == null || requiredRole.isEmpty()) {
            return false;
        }
        
        String roleVar = config.getRoleSessionVariable();
        Object roleValue = session.getAttribute(roleVar);
        
        if (roleValue == null) {
            return false;
        }
        
        // Cas 1: Le rôle est une String simple
        if (roleValue instanceof String) {
            return requiredRole.equalsIgnoreCase((String) roleValue);
        }
        
        // Cas 2: Les rôles sont un tableau de String
        if (roleValue instanceof String[]) {
            for (String role : (String[]) roleValue) {
                if (requiredRole.equalsIgnoreCase(role)) {
                    return true;
                }
            }
            return false;
        }
        
        // Cas 3: Les rôles sont une Collection
        if (roleValue instanceof java.util.Collection) {
            for (Object role : (java.util.Collection<?>) roleValue) {
                if (role != null && requiredRole.equalsIgnoreCase(role.toString())) {
                    return true;
                }
            }
            return false;
        }
        
        return false;
    }
}
