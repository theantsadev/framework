package servlet.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Configuration du mapper
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Convertit un objet Java en JSON
     */
    public static String toJson(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    /**
     * Parse un JSON en objet Java
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return mapper.readValue(json, clazz);
    }

    /**
     * Obtenir le mapper pour usage avancé
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }
}
