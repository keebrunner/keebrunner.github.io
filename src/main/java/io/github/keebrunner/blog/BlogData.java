package io.github.keebrunner.blog;

import org.springframework.stereotype.Component;

@Component
class BlogData {
    public String getTitle() {
        return "My Awesome Blog";
    }

    public String getUrl() {
        return "http://localhost:8080/generate"; // Замените на ваш URL
    }

    public String getDescription() {
        return "This is my awesome blog!";
    }

    public String getArticle() {
        return "<h1 class=text-2xl>Сатья</h1>";
    }
}