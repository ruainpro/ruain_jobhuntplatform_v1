package com.dao.rjobhunt.others;


import java.util.Map;
import java.util.regex.Pattern;

public class ActionEntityResolver {

    private static final Map<Pattern, String> ENTITY_MAP = Map.of(
        Pattern.compile("^/auth/.*"), "USER",
        Pattern.compile("^/profile/.*"), "PROFILE",
        Pattern.compile("^/jobs/.*"), "JOB",
        Pattern.compile("^/template/.*"), "TEMPLATE",
        Pattern.compile("^/notifications/.*"), "NOTIFICATION",
        Pattern.compile("^/admin/.*"), "ADMIN"
    );

    public static String resolve(String path) {
        if (path == null) return "UNKNOWN";

        return ENTITY_MAP.entrySet().stream()
            .filter(entry -> entry.getKey().matcher(path).matches())
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("UNKNOWN");
    }
}