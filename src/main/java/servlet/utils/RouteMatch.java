package servlet.utils;

import java.util.Map;

public class RouteMatch {
    private MethodInvoker method;
    private Map<String, String> pathParams;

    public RouteMatch(MethodInvoker method, Map<String, String> params) {
        this.method = method;
        this.pathParams = params;
    }

    public MethodInvoker getMethodInvoker() {
        return method;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

}
