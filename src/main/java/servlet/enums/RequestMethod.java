package servlet.enums;

public enum RequestMethod {
    GET,
    POST;

    public static RequestMethod fromString(String method) {
        try {
            return RequestMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Méthode HTTP inconnue : " + method);
        }
    }
}
