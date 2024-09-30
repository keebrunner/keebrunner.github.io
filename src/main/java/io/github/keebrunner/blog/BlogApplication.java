package io.github.keebrunner.blog;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SpringBootApplication
public class BlogApplication {
    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(BlogApplication.class, args);
        HtmlController controller = context.getBean(HtmlController.class);

        // Генерируем index.html
        String generatedIndexHtml = controller.generateIndexHtml();
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath("index.html"), generatedIndexHtml.getBytes());

        // Генерируем 2024-10-02-my-blog-post.html
        String generatedPostHtml = controller.generatePostHtml();
        String outputPostFileName = controller.getOutputFileName();
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath(outputPostFileName), generatedPostHtml.getBytes());

        // Генерируем blog.html
        String generatedBlogHtml = controller.generateBlogHtml();
        Files.write(context.getBean(PathsConfig.class).getBlogOutputFilePath(), generatedBlogHtml.getBytes());

        System.exit(0);
    }
}

@Configuration
class AppConfig {
    @Bean
    public HtmlGenerator htmlGenerator() {
        return new HtmlGenerator();
    }
}

@RestController
class HtmlController {
    @Autowired
    private HtmlGenerator htmlGenerator;
    @Autowired
    private PathsConfig pathsConfig;

    @Getter
    private String outputFileName;

    @GetMapping("/generate-index")
    public String generateIndexHtml() throws IOException {
        return generateHtmlFromMarkdown(pathsConfig.getIndexFilePath(), pathsConfig.getBaseFilePath());
    }

    @GetMapping("/generate-post")
    public String generatePostHtml() throws IOException {
        return generateHtmlFromMarkdown(pathsConfig.getContentFilePath(), pathsConfig.getPostFilePath()); // Используем post.html
    }

    @GetMapping("/generate-blog")
    public String generateBlogHtml() throws IOException {
        Map<String, Object> blogData = new HashMap<>();

        List<Map<String, String>> posts = getAllPosts();
        blogData.put("posts", posts);

        Path blogTemplatePath = pathsConfig.getBlogTemplatePath();
        String blogTemplate = new String(Files.readAllBytes(blogTemplatePath));

        String blogHtml = htmlGenerator.generateHtmlContent(blogTemplate, blogData);

        return blogHtml;
    }

    private String generateHtmlFromMarkdown(Path markdownPath, Path templatePath) throws IOException {
        Map<String, Object> data = new HashMap<>();

        String content = new String(Files.readAllBytes(markdownPath));

        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        String[] parts = content.split("---", 3);
        String yamlFrontMatter = parts[1];
        String markdownContent = parts[2];

        Yaml yaml = new Yaml();
        Map<String, Object> metadata = yaml.load(yamlFrontMatter);

        java.util.Date date = (java.util.Date) metadata.get("date");
        LocalDateTime localDateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
        String formattedDate = localDateTime.format(formatter);

        markdownContent = markdownContent.replace("{{date}}", formattedDate);

        Document document = parser.parse(markdownContent);
        String article = renderer.render(document);

        data.put("title", (String) metadata.get("title"));
        data.put("description", (String) metadata.get("description"));
        data.put("article", article);
        data.put("url", (String) metadata.get("url"));

        this.outputFileName = (String) metadata.get("url");

        String template = new String(Files.readAllBytes(templatePath));

        List<Map<String, String>> latestPosts = getLatestPosts(parser, renderer);
        data.put("latestPosts", latestPosts);

        String html = htmlGenerator.generateHtmlContent(template, data);

        return html;
    }

    private List<Map<String, String>> getLatestPosts(Parser parser, HtmlRenderer renderer) throws IOException {
        List<Map<String, String>> latestPosts = new ArrayList<>();
        Path blogDir = pathsConfig.getBlogDirPath();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(blogDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && entry.toString().endsWith(".md")) {
                    String postContent = new String(Files.readAllBytes(entry));

                    String[] parts = postContent.split("---", 3);
                    String yamlFrontMatter = parts[1];

                    Yaml yaml = new Yaml();
                    Map<String, Object> metadata = yaml.load(yamlFrontMatter);

                    java.util.Date date = (java.util.Date) metadata.get("date");
                    LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
                    String formattedDate = localDateTime.format(formatter);

                    Map<String, String> postMetadata = new HashMap<>();
                    postMetadata.put("date", formattedDate);
                    postMetadata.put("title", (String) metadata.get("title"));
                    postMetadata.put("url", (String) metadata.get("url"));

                    latestPosts.add(postMetadata);
                }
            }
        }

        latestPosts.sort((p1, p2) -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
            LocalDateTime date1 = LocalDateTime.parse(p1.get("date"), formatter);
            LocalDateTime date2 = LocalDateTime.parse(p2.get("date"), formatter);
            return date2.compareTo(date1); // Сортировка по убыванию даты
        });

        return latestPosts.subList(0, Math.min(latestPosts.size(), 3)); // Возвращаем не более 3 последних постов
    }

    private List<Map<String, String>> getAllPosts() throws IOException {
        List<Map<String, String>> allPosts = new ArrayList<>();
        Path blogDir = pathsConfig.getBlogDirPath();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(blogDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && entry.toString().endsWith(".md")) {
                    String postContent = new String(Files.readAllBytes(entry));

                    String[] parts = postContent.split("---", 3);
                    String yamlFrontMatter = parts[1];

                    Yaml yaml = new Yaml();
                    Map<String, Object> metadata = yaml.load(yamlFrontMatter);

                    Map<String, String> postMetadata = new HashMap<>();
                    postMetadata.put("title", (String) metadata.get("title"));
                    postMetadata.put("url", (String) metadata.get("url"));

                    allPosts.add(postMetadata);
                }
            }
        }

        return allPosts;
    }
}

@Component
class HtmlGenerator {
    public HtmlGenerator() {
    }

    public String generateHtmlContent(String template, Map<String, Object> data) {
        template = template.replaceAll("th:content", "content");

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> listValue = (List<?>) value;
                StringBuilder listHtml = new StringBuilder();
                for (Object item : listValue) {
                    if (item instanceof Map) {
                        Map<String, String> itemData = (Map<String, String>) item;
                        listHtml.append("<li class=\"post-item\">");
                        listHtml.append("<a href=\"/blog/").append(itemData.get("url")).append("\">");
                        listHtml.append(itemData.get("title"));
                        listHtml.append("</a></li>");
                    }
                }
                template = template.replace("${" + key + "}", listHtml.toString());
            } else {
                template = template.replace("${" + key + "}", value.toString());
            }
        }
        return template;
    }
}

@Configuration
class PathsConfig {
    private final Path home = Path.of("C:", "Users", "keebrunner");

    public Path getOutputFilePath(String fileName) {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", fileName));
    }

    public Path getBaseFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "base.html"));
    }

    public Path getContentFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "content.md"));
    }

    public Path getBlogDirPath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "articles"));
    }

    public Path getBlogTemplatePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "base-blog.html"));
    }

    public Path getBlogOutputFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "blog.html"));
    }

    public Path getIndexFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "index.md"));
    }

    public Path getPostFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "post.html"));
    }
}