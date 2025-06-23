package com.dao.rjobhunt.others;

public class RequestContext {
    private static final ThreadLocal<String> ip = new ThreadLocal<>();
    private static final ThreadLocal<String> userAgent = new ThreadLocal<>();
    private static final ThreadLocal<String> requestPath = new ThreadLocal<>();
    private static final ThreadLocal<String> httpMethod = new ThreadLocal<>();
    private static final ThreadLocal<String> routePattern = new ThreadLocal<>();
    private static final ThreadLocal<String> controllerMethod = new ThreadLocal<>();

    public static void set(String ipAddr, String ua, String path, String method, String route, String ctrlMethod) {
        ip.set(ipAddr);
        userAgent.set(ua);
        requestPath.set(path);
        httpMethod.set(method);
        routePattern.set(route);
        controllerMethod.set(ctrlMethod);
    }

    public static String getIp() { return ip.get(); }
    public static String getUserAgent() { return userAgent.get(); }
    public static String getRequestPath() { return requestPath.get(); }
    public static String getHttpMethod() { return httpMethod.get(); }
    public static String getRoutePattern() { return routePattern.get(); }
    public static String getControllerMethod() { return controllerMethod.get(); }

    public static void clear() {
        ip.remove();
        userAgent.remove();
        requestPath.remove();
        httpMethod.remove();
        routePattern.remove();
        controllerMethod.remove();
    }
}