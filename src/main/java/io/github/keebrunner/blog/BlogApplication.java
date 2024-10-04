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
import java.time.Instant;
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

        // Генерируем java.html
        String generatedJavaHtml = controller.generateJavaHtml();
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath("java.html"), generatedJavaHtml.getBytes());

        // Генерируем metrics.html
        String generatedMetricsHtml = controller.generateMetricsHtml();
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath("metrics.html"), generatedMetricsHtml.getBytes());

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

    @GetMapping("/generate-java")
    public String generateJavaHtml() throws IOException {
        Path orgPath = pathsConfig.getJavaOrgFilePath();
        Path templatePath = pathsConfig.getOrgTemplatePath();

        String content = new String(Files.readAllBytes(orgPath));
        String template = new String(Files.readAllBytes(templatePath));

        Map<String, Object> data = new HashMap<>();
        data.put("orgContent", generateOrgList(content)); // Генерируем HTML-список

        String html = htmlGenerator.generateHtmlContent(template, data);

        return html;
    }

    @GetMapping("/generate-metrics")
    public String generateMetricsHtml() throws IOException {
        Path metricsPath = pathsConfig.getMetricsFilePath();
        Path templatePath = pathsConfig.getMetricsTemplatePath();

        String content = new String(Files.readAllBytes(metricsPath));
        String template = new String(Files.readAllBytes(templatePath));

        Map<String, Object> data = new HashMap<>();
        data.put("metrics", generateMetricsList(content)); // Генерируем HTML-список метрик

        String html = htmlGenerator.generateHtmlContent(template, data);

        return html;
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

        Instant instant = ((java.util.Date) metadata.get("date")).toInstant();

        // Форматируем Instant с помощью DateTimeFormatter, учитывая UTC
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH)
                .withZone(ZoneId.of("UTC")); // Указываем UTC для форматирования

        String formattedDate = formatter.format(instant);

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

                    // Форматируем дату для URL
                    DateTimeFormatter urlDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                    String urlFormattedDate = localDateTime.format(urlDateFormatter);

                    Map<String, String> postMetadata = new HashMap<>();
                    postMetadata.put("date", formattedDate);
                    postMetadata.put("title", urlFormattedDate + " - " + (String) metadata.get("title")); // Добавляем дату к заголовку
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

                    java.util.Date date = (java.util.Date) metadata.get("date");
                    LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
                    String formattedDate = localDateTime.format(formatter); // Форматируем дату для URL

                    Map<String, String> postMetadata = new HashMap<>();
                    postMetadata.put("title", formattedDate + " - " + (String) metadata.get("title")); // Добавляем дату к заголовку
                    postMetadata.put("url", (String) metadata.get("url"));

                    allPosts.add(postMetadata);
                }
            }
        }

        return allPosts;
    }

    private String generateOrgList(String content) {
        StringBuilder html = new StringBuilder();
        String[] lines = content.split("\n");
        int level = 0; // Отслеживаем уровень вложенности

        for (String line : lines) {
            if (line.startsWith("* ")) {
                // Начало элемента первого уровня
                if (level > 0) {
                    // Закрываем предыдущие списки
                    for (int i = 0; i < level - 1; i++) {
                        html.append("</ul></li>");
                    }
                    html.append("</ul></li>");
                }
                html.append("<li><span class=\"cursor-pointer level1\" onclick=\"toggleList(this)\"><strong>* </strong>").append(line.substring(2)).append("</span><ul style=\"display: none;\">");
                level = 1;
            } else if (line.startsWith("** ")) {
                // Начало элемента второго уровня
                if (level == 1) {
                    html.append("<li><span class=\"cursor-pointer level2\" onclick=\"toggleList(this)\"><strong>** </strong>").append(line.substring(3)).append("</span><ul style=\"display: none;\">");
                    level = 2;
                } else if (level == 2) {
                    html.append("</ul></li><li><span class=\"cursor-pointer level2\" onclick=\"toggleList(this)\"><strong>** </strong>").append(line.substring(3)).append("</span><ul style=\"display: none;\">");
                }
            } else if (line.startsWith("*** ")) {
                // Начало элемента третьего уровня
                if (level == 2) {
                    html.append("<li class=\"level3\"><strong>*** </strong>").append(line.substring(4)).append("</li>");
                }
            }
        }

        // Закрываем все открытые списки
        for (int i = 0; i < level; i++) {
            html.append("</ul></li>");
        }

        return html.toString();
    }

    private String generateMetricsList(String content) {
        StringBuilder html = new StringBuilder();
        String[] lines = content.split("\n");

        for (String line : lines) {
            html.append("<li>").append(line).append("</li>");
        }

        return html.toString();
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
                        listHtml.append(itemData.get("title")); // Используем измененный заголовок с датой
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

    public Path getJavaOrgFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "java.org"));
    }

    public Path getOrgTemplatePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "org.html"));
    }

    public Path getMetricsFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "metrics.md"));
    }

    public Path getMetricsTemplatePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "base-metrics.html"));
    }
}