package com.dao.rjobhunt.others;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.io.IOException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


import org.springframework.core.io.ClassPathResource;


@Component
public class RequestUtil {

    public String getBaseUrl() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current request attributes found");
        }

        HttpServletRequest request = attributes.getRequest();
        String scheme = request.getScheme(); // http or https
        String host = request.getServerName(); // localhost or domain
        int port = request.getServerPort();    // port
        return scheme + "://" + host + ((port == 80 || port == 443) ? "" : ":" + port);
    }

    
    public static String loadTemplate(String path) throws java.io.IOException {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new FileNotFoundException("Template not found at path: " + path);
        }

        // Print the resolved file path (for debugging)
        System.out.println("Resolved template path: " + resource.getFile().getAbsolutePath());

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
}