package io.github.keebrunner.blog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(BlogApplication.class, args);

        // 1. Получаем экземпляр HtmlController
        HtmlController controller = context.getBean(HtmlController.class);

        // 2. Вызываем метод generateHtml
        String html = controller.generateHtml();

        // 3. Сохраняем HTML
        Files.write(context.getBean(PathsConfig.class).getOutputFilePath(), html.getBytes());

        // 4. Закрываем приложение
        System.exit(0);
    }
}

@Configuration
class AppConfig {

    @Bean
    public BasicDataFetcher basicDataFetcher() {
        return new BasicDataFetcher();
    }

    @Bean
    public HtmlGenerator htmlGenerator(BasicDataFetcher basicDataFetcher) {
        return new HtmlGenerator(basicDataFetcher);
    }
}

@RestController
class HtmlController {
    @Autowired
    private HtmlGenerator htmlGenerator;
    @Autowired
    private PathsConfig pathsConfig;

    @GetMapping("/generate")
    public String generateHtml() throws IOException {
        // 1.  Получаем  данные  для  каждой  страницы
        Map<String, String> basicData = new HashMap<>();
        basicData.put("title", "My Awesome Blog");

        // 2.  Загружаем  шаблоны  для  каждой  страницы
        Path templatePath = pathsConfig.getTemplateFilePath();
        String basicTemplate = new String(Files.readAllBytes(templatePath));

        // 3.  Генерируем  HTML  для  каждой  страницы
        String basicHtml = htmlGenerator.generateHtmlContent(basicTemplate, basicData);

        return basicHtml;
    }
}

@Component
class HtmlGenerator {
    private BasicDataFetcher basicDataFetcher;

    public HtmlGenerator(BasicDataFetcher basicDataFetcher) {
        this.basicDataFetcher = basicDataFetcher;
    }

    public String generateHtmlContent(String template, Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            template = template.replace("${" + key + "}", value);
        }
        return template;
    }
}

@Component
class BasicDataFetcher {
    public Map<String, String> fetchData() {
        Map<String, String> data = new HashMap<>();
        data.put("title", "My Awesome Blog");
        return data;
    }
}

@Configuration
class PathsConfig {

    private final Path home = Path.of("C:", "Users", "keebrunner");

    public Path getTemplateFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "template.html"));
    }

    public Path getOutputFilePath() {
        return home.resolve(Path.of("IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog", "output.html"));
    }
}