package servlet.utils;

public class MethodInvoker {
    Class<?> controllerClass;
    java.lang.reflect.Method method;

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    public java.lang.reflect.Method getMethod() {
        return method;
    }

    public void setMethod(java.lang.reflect.Method method) {
        this.method = method;
    }

    public MethodInvoker(Class<?> c, java.lang.reflect.Method m) {
        this.controllerClass = c;
        this.method = m;
    }

    public Object execute(Object... args) throws Exception {
        Object object = controllerClass.getDeclaredConstructor().newInstance();
        return method.invoke(object, args);
    }
}
