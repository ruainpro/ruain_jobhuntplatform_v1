package com.dao.rjobhunt.others;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public class YamlValidator {

    public static boolean isValidYaml(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            return false;
        }
        try {
            Yaml yaml = new Yaml();
            yaml.load(yamlContent);
            return true; // No exception → YAML is valid
        } catch (YAMLException e) {
            return false; // Parsing failed → invalid YAML
        }
    }
}