package com.dao.rjobhunt.others;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Optional;

@Component
public class RequestMetadataInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String httpMethod = request.getMethod();

        // Pattern-matched URI like "/auth/login" instead of raw "/auth/login?token=123"
        String routePattern = Optional.ofNullable(
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
        ).map(Object::toString).orElse("UNKNOWN");

        String handlerMethod = "UNKNOWN";
        if (handler instanceof HandlerMethod handlerMethodObj) {
            handlerMethod = handlerMethodObj.getMethod().getDeclaringClass().getSimpleName() +
                            "." +
                            handlerMethodObj.getMethod().getName();
        }

        RequestContext.set(ip, userAgent, request.getRequestURI(), httpMethod, routePattern, handlerMethod);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestContext.clear();
    }
}