package servlet;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class ModelView {
    String view;
    Map<String, Object> attributes;

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public ModelView(String view) {
        this.view = view;
    }

    public ModelView() {
        attributes = new HashMap<>();

    }

    public void passVar(HttpServletRequest request) {
        if (attributes == null)
            return;

        for (HashMap.Entry<String, Object> entry : attributes.entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public void addAttribute(String key, Object object) {
        attributes.put(key, object);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
