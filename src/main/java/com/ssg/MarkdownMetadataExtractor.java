package com.ssg;

import java.util.*;
import java.util.regex.*;

public class MarkdownMetadataExtractor {

    private static final Pattern METADATA_PATTERN =
            Pattern.compile("^---\\s*\\n(.*?)\\n---", Pattern.DOTALL);

    public static Map<String, String> extractMetadata(String markdown) {
        Matcher matcher = METADATA_PATTERN.matcher(markdown);
        Map<String, String> metadata = new HashMap<>();

        if (matcher.find()) {
            String[] lines = matcher.group(1).split("\n");
            for (String line : lines) {
                int colon = line.indexOf(':');
                if (colon != -1) {
                    String key = line.substring(0, colon).trim();
                    String value = line.substring(colon + 1).trim();
                    metadata.put(key, value);
                }
            }
        }

        return metadata;
    }

    public static String stripMetadata(String markdown) {
        Pattern pattern = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n?", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(markdown);
        if (matcher.find()) {
            return markdown.substring(matcher.end()).stripLeading();
        }
        return markdown;
    }

}
