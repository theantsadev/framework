package servlet.security;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration de sécurité chargée depuis security.properties
 * Définit les noms des variables de session pour l'authentification et les rôles
 */
public class SecurityConfig {
    
    private static final String CONFIG_FILE = "security.properties";
    
    // Noms des clés dans le fichier properties
    private static final String AUTH_SESSION_KEY = "security.session.auth";
    private static final String ROLE_SESSION_KEY = "security.session.role";
    
    // Valeurs par défaut si non configurées
    private static final String DEFAULT_AUTH_VAR = "auth";
    private static final String DEFAULT_ROLE_VAR = "role";
    
    private String authSessionVariable;
    private String roleSessionVariable;
    
    private static SecurityConfig instance;
    
    private SecurityConfig() {
        loadConfig();
    }
    
    public static synchronized SecurityConfig getInstance() {
        if (instance == null) {
            instance = new SecurityConfig();
        }
        return instance;
    }
    
    private void loadConfig() {
        Properties props = new Properties();
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                System.out.println("[SecurityConfig] Fichier " + CONFIG_FILE + " chargé");
            } else {
                System.out.println("[SecurityConfig] Fichier " + CONFIG_FILE + " non trouvé, utilisation des valeurs par défaut");
            }
        } catch (Exception e) {
            System.err.println("[SecurityConfig] Erreur lors du chargement: " + e.getMessage());
        }
        
        // Charger les valeurs ou utiliser les valeurs par défaut
        authSessionVariable = props.getProperty(AUTH_SESSION_KEY, DEFAULT_AUTH_VAR);
        roleSessionVariable = props.getProperty(ROLE_SESSION_KEY, DEFAULT_ROLE_VAR);
        
        System.out.println("[SecurityConfig] Variable auth: " + authSessionVariable);
        System.out.println("[SecurityConfig] Variable role: " + roleSessionVariable);
    }
    
    /**
     * Retourne le nom de la variable de session pour l'authentification
     */
    public String getAuthSessionVariable() {
        return authSessionVariable;
    }
    
    /**
     * Retourne le nom de la variable de session pour le rôle
     */
    public String getRoleSessionVariable() {
        return roleSessionVariable;
    }
    
    /**
     * Recharge la configuration (utile pour les tests)
     */
    public void reload() {
        loadConfig();
    }
}
