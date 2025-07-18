package com.ssg;

import com.ssmp.parser.MarkdownParser;
import com.ssmp.renderer.HTMLRenderer;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class StaticSiteGenerator {
    private final String outputDir;

    public StaticSiteGenerator(String outputDir) {
        this.outputDir = outputDir;
    }

    public void generateSite() throws IOException {
        // Clear output directory if it exists, then create it
        clearOutputDirectory();
        Files.createDirectories(Paths.get(outputDir));

        // Generate home page
        generateHomePage();

        // Generate posts page
        generatePostsPage();

        // Generate individual blog posts
        generateBlogPosts();

        // Copy static assets
        copyStaticAssets();

        System.out.println("Site generated successfully in: " + outputDir);
    }

    private void generateHomePage() throws IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("activeHome", "active");
        replacements.put("activePosts", "");
        replacements.put("content", readContentFile("about.html"));

        String html = processTemplate("base.html", replacements);
        writeFile(outputDir + "/about.html", html);
        System.out.println("Generated: about.html");
    }

    private void generatePostsPage() throws IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("activeHome", "");
        replacements.put("activePosts", "active");
        replacements.put("content", readContentFile("posts.html"));
        replacements.put("blog_list", generateBlogList());

        String html = processTemplate("base.html", replacements);
        writeFile(outputDir + "/posts.html", html);
        System.out.println("Generated: posts.html");
    }

    private void generateBlogPosts() throws IOException {
        // Get posts from resources
        try {
            var postsPath = getClass().getResource("/content/posts");
            if (postsPath == null) return;

            File postsDir = new File(postsPath.toURI());
            File[] postFiles = postsDir.listFiles((dir, name) -> name.endsWith(".md"));
            if (postFiles == null) return;

            // Create posts output directory
            Files.createDirectories(Paths.get(outputDir+ "/posts"));

            for (File postFile : postFiles) {
                String markdown = Files.readString(postFile.toPath());

                Map<String, String> metadata = MarkdownMetadataExtractor.extractMetadata(markdown);
                String strippedMarkdown = MarkdownMetadataExtractor.stripMetadata(markdown);

                // Use your custom parser to convert Markdown to HTML
                MarkdownParser blockParser = MarkdownParser.fromString(strippedMarkdown);
                blockParser.generateParseTree();
                HTMLRenderer htmlRenderer = new HTMLRenderer(blockParser.getBlockTrees());

                String htmlBody = htmlRenderer.renderHtml(); // Replace this with your parser call


                Map<String, String> replacements = new HashMap<>();
                replacements.put("activeHome", "");
                replacements.put("activePosts", "active");


                replacements.put("content", htmlBody);
                replacements.put("blog_list", ""); // Empty for individual pages

                String renderedHtml = processTemplate("base.html", replacements);
                String updatedRenderedHtml = renderedHtml.replace("static", "../../static");

                String outputName = postFile.getName().replace(".md", "");
                writeFile(outputDir + "/posts/" + outputName + "/index.html", updatedRenderedHtml);
                System.out.println("Generated: posts/" + outputName);
            }

        } catch (Exception e) {
            System.err.println("Error generating blog posts: " + e.getMessage());
        }
    }

    private String generateBlogList() throws IOException {
        try {
            var postsPath = getClass().getResource("/content/posts");
            if (postsPath == null) {
                return "<p>No blog posts found.</p>";
            }

            File postsDir = new File(postsPath.toURI());


            StringBuilder blogList = new StringBuilder();
            blogList.append("<section>\n");

            File[] postFiles = postsDir.listFiles((dir, name) -> name.endsWith(".md"));
            if (postFiles == null || postFiles.length == 0) {
                return "<p>No blog posts found.</p>";
            }


            List<File> sortedPostFiles = Arrays.stream(postFiles)
                    .sorted((f1, f2) -> {
                        try {
                            String md1 = Files.readString(f1.toPath());
                            String md2 = Files.readString(f2.toPath());
                            String d1 = MarkdownMetadataExtractor.extractMetadata(md1).getOrDefault("date", "");
                            String d2 = MarkdownMetadataExtractor.extractMetadata(md2).getOrDefault("date", "");
                            return d2.compareTo(d1); // newest first
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();



            for (File postFile : sortedPostFiles) {
                String markdown = Files.readString(postFile.toPath());
                Map<String, String> metadata = MarkdownMetadataExtractor.extractMetadata(markdown);

                String title = metadata.getOrDefault("title", postFile.getName().replace(".md", ""));
                String date = metadata.getOrDefault("date", "Unknown date");
                String description = metadata.getOrDefault("description", "");

                String fileName = postFile.getName().replace(".md", ".html");

                blogList.append("  <article class=\"blog-preview\">\n");
                blogList.append("    <h3><a href=\"/posts/" + fileName.replace(".html", "") + "\">" + title + "</a></h3>\n");
                blogList.append("    <p class=\"meta\"><small>" + date + "</small></p>\n");
                blogList.append("  </article>\n");
            }


            blogList.append("</ul>\n</section>");
            return blogList.toString();
        } catch (Exception e) {
            System.err.println("Error generating blog list: " + e.getMessage());
            return "<p>Error loading blog posts.</p>";
        }
    }

    private String extractTitleFromPost(File postFile) throws IOException {
        String content = Files.readString(postFile.toPath());
        // Simple title extraction - look for first <h1> tag
        int start = content.indexOf("<h1>");
        int end = content.indexOf("</h1>");

        if (start != -1 && end != -1) {
            return content.substring(start + 4, end).trim();
        }

        // Fallback to filename without extension
        return postFile.getName().replace(".html", "").replace("-", " ");
    }

    private String processTemplate(String templateName, Map<String, String> replacements)
            throws IOException {
        try {
            String templateContent = readResourceFile("/templates/" + templateName);
            TemplateEngine engine = TemplateEngine.fromString(templateContent);
            return engine.render(replacements);
        } catch (Exception e) {
            throw new IOException("Template not found: " + templateName, e);
        }
    }

    private String readContentFile(String fileName) throws IOException {
        try {
            return readResourceFile("/content/" + fileName);
        } catch (IOException e) {
            System.err.println("Content file not found: " + fileName + ", using empty content");
            return "";
        }
    }

    private String readResourceFile(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }

    private void copyStaticAssets() throws IOException {
        try {
            var staticResourceUrl = getClass().getResource("/static");
            if (staticResourceUrl != null) {
                Path staticSourcePath = Paths.get(staticResourceUrl.toURI());
                Path staticTargetPath = Paths.get(outputDir + "/static");

                // Copy the entire static directory contents to outputDir/static/
                copyDirectoryContents(staticSourcePath, staticTargetPath);
                System.out.println("Copied all static assets from /static to " + outputDir + "/static");
            } else {
                System.err.println("Static resource directory not found: /static");
            }
        } catch (Exception e) {
            System.err.println("Error copying static assets: " + e.getMessage());
        }
    }

    private void copyDirectoryContents(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path relativePath = source.relativize(sourcePath);
                Path targetPath = target.resolve(relativePath);

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    // Ensure parent directories exist
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied: " + relativePath);
                }
            } catch (IOException e) {
                System.err.println("Failed to copy: " + sourcePath + " - " + e.getMessage());
            }
        });
    }

    private void clearOutputDirectory() throws IOException {
        Path outputPath = Paths.get(outputDir);
        if (Files.exists(outputPath)) {
            System.out.println("Clearing existing output directory: " + outputDir);
            Files.walk(outputPath)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    private void writeFile(String filePath, String content) throws IOException {
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.writeString(Paths.get(filePath), content);
    }
}