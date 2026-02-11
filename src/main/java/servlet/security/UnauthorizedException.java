package servlet.security;

/**
 * Exception levée quand l'utilisateur n'est pas authentifié (HTTP 401)
 */
public class UnauthorizedException extends SecurityException {
    
    public UnauthorizedException() {
        super("Authentification requise");
    }
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public int getStatusCode() {
        return 401;
    }
}
