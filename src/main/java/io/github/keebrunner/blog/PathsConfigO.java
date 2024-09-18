package io.github.keebrunner.blog;

import java.nio.file.Path;

public class PathsConfigO {

    private final Path root = Path.of("C:", "Users", "keebrunner", "IdeaProjects", "blog", "src", "main", "java", "io", "github", "keebrunner", "blog");

    public Path getTemplateFilePath() {
        return root.resolve(Path.of("template.html"));
    }

    public Path getOutputFilePath() {
        return root.resolve(Path.of("output.html"));
    }
}
