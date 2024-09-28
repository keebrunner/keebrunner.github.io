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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(BlogApplication.class, args);

        // 1. Получаем экземпляр HtmlController
        HtmlController controller = context.getBean(HtmlController.class);

        // 2. Вызываем метод generateHtml
        String generatedHtml = controller.generateHtml();

        // 3. Получаем имя файла из метаданных
        String outputFileName = controller.getOutputFileName();

        // 4. Сохраняем HTML с именем файла из метаданных
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath(outputFileName), generatedHtml.getBytes());

        // 5. Закрываем приложение
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

    // Метод для получения имени файла
    @Getter
    private String outputFileName; // Поле для хранения имени файла

    @GetMapping("/generate")
    public String generateHtml() throws IOException {
        // 1.  Получаем  данные  для  каждой  страницы
        Map<String, String> basicData = new HashMap<>();

        // 2.  Загружаем  контент из content.md
        Path contentPath = pathsConfig.getContentFilePath();
        String content = new String(Files.readAllBytes(contentPath));

        // 3.  Парсим Markdown и извлекаем метаданные и контент
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // Разделяем YAML front matter и контент Markdown
        String[] parts = content.split("---", 3);
        String yamlFrontMatter = parts[1];
        String markdownContent = parts[2];

        // Парсим YAML front matter
        Yaml yaml = new Yaml();
        Map<String, Object> metadata = yaml.load(yamlFrontMatter);

        // Получаем дату из метаданных (как java.util.Date)
        java.util.Date date = (java.util.Date) metadata.get("date");

        // Преобразуем java.util.Date в LocalDateTime
        LocalDateTime localDateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Форматируем дату с использованием английской локали
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
        String formattedDate = localDateTime.format(formatter);

        // Вставляем дату в контент Markdown
        markdownContent = markdownContent.replace("{{date}}", formattedDate);

        // Рендерим Markdown контент в HTML (с вставленной датой)
        Document document = parser.parse(markdownContent);
        String article = renderer.render(document);

        basicData.put("title", (String) metadata.get("title"));
        basicData.put("description", (String) metadata.get("description"));
        basicData.put("article", article);
        basicData.put("url", (String) metadata.get("url")); // Получаем URL из метаданных

        // Сохраняем имя файла из метаданных
        this.outputFileName = (String) metadata.get("url");

        // 4.  Загружаем  шаблоны  для  каждой  страницы
        Path baseTemplatePath = pathsConfig.getBaseFilePath();
        String baseTemplate = new String(Files.readAllBytes(baseTemplatePath));

        // 5.  Генерируем  HTML  для  каждой  страницы
        String basicHtml = htmlGenerator.generateHtmlContent(baseTemplate, basicData);

        return basicHtml;
    }

}


@Component
class HtmlGenerator {

    public HtmlGenerator() {
    }

    public String generateHtmlContent(String template, Map<String, String> data) {
        template = template.replaceAll("th:content", "content");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            template = template.replace("${" + key + "}", value);
        }
        return template;
    }
}

@Configuration
class PathsConfig {

    private final Path home = Path.of("C:", "Users", "keebrunner");

    // Метод для получения пути к выходному файлу с именем файла
    public Path getOutputFilePath(String fileName) {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "blog", fileName));
    }

    public Path getBaseFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "base.html"));
    }

    public Path getContentFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "content.md"));
    }
}