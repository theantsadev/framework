package servlet.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire centralisé des sessions
 * Stocke les sessions en mémoire et nettoie automatiquement les sessions
 * expirées
 */
public class SessionManager {
    private static final Map<String, CustomSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService CLEANUP_SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    private static boolean cleanupStarted = false;

    /**
     * Initialise le nettoyage automatique des sessions expirées
     * Appelé automatiquement lors de la première utilisation
     */
    public static void startCleanupTask() {
        if (!cleanupStarted) {
            CLEANUP_SCHEDULER.scheduleAtFixedRate(() -> {
                cleanupExpiredSessions();
            }, 5, 5, TimeUnit.MINUTES); // Nettoyage toutes les 5 minutes
            cleanupStarted = true;
        }
    }

    /**
     * Crée une nouvelle session avec un ID unique
     */
    public static CustomSession createSession() {
        String sessionId = generateSessionId();
        CustomSession session = new CustomSession(sessionId);
        SESSIONS.put(sessionId, session);
        startCleanupTask();
        return session;
    }

    /**
     * Récupère une session existante par son ID
     * Retourne null si la session n'existe pas ou est expirée
     */
    public static CustomSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        CustomSession session = SESSIONS.get(sessionId);

        if (session != null && session.isExpired()) {
            SESSIONS.remove(sessionId);
            return null;
        }

        return session;
    }

    /**
     * Récupère une session existante ou en crée une nouvelle si elle n'existe pas
     */
    public static CustomSession getOrCreateSession(String sessionId) {
        if (sessionId != null) {
            CustomSession session = getSession(sessionId);
            if (session != null) {
                return session;
            }
        }
        return createSession();
    }

    /**
     * Invalide et supprime une session
     */
    public static void invalidateSession(String sessionId) {
        if (sessionId != null) {
            CustomSession session = SESSIONS.remove(sessionId);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    /**
     * Nettoie toutes les sessions expirées
     */
    private static void cleanupExpiredSessions() {
        SESSIONS.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                System.out.println("[SessionManager] Session expirée supprimée: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Génère un ID de session unique
     */
    private static String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Retourne le nombre de sessions actives
     */
    public static int getActiveSessionCount() {
        return SESSIONS.size();
    }

    /**
     * Supprime toutes les sessions (utile pour les tests)
     */
    public static void clearAll() {
        SESSIONS.clear();
    }

    /**
     * Arrête le service de nettoyage (à appeler lors de l'arrêt de l'application)
     */
    public static void shutdown() {
        CLEANUP_SCHEDULER.shutdown();
        try {
            if (!CLEANUP_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                CLEANUP_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            CLEANUP_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}