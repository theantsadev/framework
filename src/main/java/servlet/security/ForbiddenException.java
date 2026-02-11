package servlet.security;

/**
 * Exception levée quand l'utilisateur n'a pas les droits suffisants (HTTP 403)
 */
public class ForbiddenException extends SecurityException {
    
    private String requiredRole;
    
    public ForbiddenException() {
        super("Accès interdit");
    }
    
    public ForbiddenException(String requiredRole) {
        super("Accès interdit - Rôle requis: " + requiredRole);
        this.requiredRole = requiredRole;
    }
    
    public int getStatusCode() {
        return 403;
    }
    
    public String getRequiredRole() {
        return requiredRole;
    }
}
