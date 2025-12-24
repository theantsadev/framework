package servlet.binding;

public class TypeConverter {

    public static Object convert(String value, Class<?> targetType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (targetType == String.class)
            return value;
        if (targetType == int.class || targetType == Integer.class)
            return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class)
            return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class)
            return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class)
            return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(value);
        return value; // fallback
    }
}