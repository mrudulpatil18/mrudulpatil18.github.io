package com.ssg;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


class TemplateEngine{
    String template;

    TemplateEngine(String template) {
        this.template = template;
    }

    public static TemplateEngine fromFile(String templatePath) throws FileNotFoundException {
        return new TemplateEngine(readFile(templatePath));
    }

    public static TemplateEngine fromString(String template) {
        return new TemplateEngine(template);
    }

    String render(Map<String, String> replacements) {
        String result = template;
        for (var entry : replacements.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public static String readFile(String fileName) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))){
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}
