package servlet.utils;

public class RegisteredRoute {
    private String httpMethod; // GET, POST
    private MethodInvoker invoker;

    public RegisteredRoute(String httpMethod, MethodInvoker invoker) {
        this.httpMethod = httpMethod;
        this.invoker = invoker;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public MethodInvoker getInvoker() {
        return invoker;
    }
}