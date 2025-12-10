package servlet.utils;

import java.util.Map;

public class RouteMatch {
    private MethodInvoker method;
    private Map<String, String> pathParams;

    public void setMethod(MethodInvoker method) {
        this.method = method;
    }

    public void setPathParams(Map<String, String> pathParams) {
        this.pathParams = pathParams;
    }

    public RouteMatch() {
    }

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
