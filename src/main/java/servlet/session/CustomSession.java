package servlet.session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Représente une session utilisateur avec ses attributs
 */
public class CustomSession {
    private String sessionId;
    private Map<String, Object> attributes;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval = 1800; // 30 minutes par défaut
    private boolean isNew;

    public CustomSession(String sessionId) {
        this.sessionId = sessionId;
        this.attributes = new ConcurrentHashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.isNew = true;
    }

    /**
     * Définit un attribut dans la session
     */
    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name cannot be null");
        }
        attributes.put(name, value);
        updateLastAccessedTime();
    }

    /**
     * Récupère un attribut de la session
     */
    public Object getAttribute(String name) {
        updateLastAccessedTime();
        return attributes.get(name);
    }

    /**
     * Récupère un attribut avec un type spécifique
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, Class<T> type) {
        Object value = getAttribute(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Attribute " + name + " is not of type " + type.getName());
    }

    /**
     * Supprime un attribut de la session
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
        updateLastAccessedTime();
    }

    /**
     * Récupère tous les noms d'attributs
     */
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[0]);
    }

    /**
     * Invalide la session (supprime tous les attributs)
     */
    public void invalidate() {
        attributes.clear();
    }

    /**
     * Vérifie si la session est expirée
     */
    public boolean isExpired() {
        long now = System.currentTimeMillis();
        long inactiveTime = (now - lastAccessedTime) / 1000; // en secondes
        return inactiveTime > maxInactiveInterval;
    }

    private void updateLastAccessedTime() {
        this.lastAccessedTime = System.currentTimeMillis();
        this.isNew = false;
    }

    // ==================== Getters & Setters ====================

    public String getSessionId() {
        return sessionId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    public boolean isNew() {
        return isNew;
    }

    @Override
    public String toString() {
        return "CustomSession{" +
                "sessionId='" + sessionId + '\'' +
                ", attributes=" + attributes.size() +
                ", isExpired=" + isExpired() +
                '}';
    }
}