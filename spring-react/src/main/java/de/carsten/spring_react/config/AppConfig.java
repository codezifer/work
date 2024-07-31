package de.carsten.spring_react.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Log4j2
public class AppConfig implements WebMvcConfigurer {

    Environment environment;

    record Tuple2(String first, String second) {
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            serveDirectory(registry, "/dev-dashboard", "classpath:/dev_assets/");
        }
        serveDirectory(registry, "/", "classpath:/static/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] mappings = properties("app.web.cors.mappings");
        String[] patterns = properties("app.web.cors.patterns");

        Stream.of(mappings).flatMap(m -> Stream.of(patterns).map(p -> new Tuple2(m, p))).forEach(t2 ->
                registry.addMapping(t2.first()).allowedOriginPatterns(t2.second())
        );
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            openApi.setServers(List.of(
                    new Server()
                            .url(environment.getProperty("app.swagger.server.url"))
                            .description(environment.getProperty("app.swagger.server.description"))
            ));
            openApi.setInfo(new Info()
                    .title(environment.getProperty("app.swagger.title"))
                    .version(environment.getProperty("app.swagger.version"))
                    .description(environment.getProperty("app.swagger.description"))
            );
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private String[] properties(String propertyName) {
        return Optional.ofNullable(environment.getProperty(propertyName))
                .map(p -> p.trim().split(","))
                .orElse(new String[0]);
    }

    private void serveDirectory(ResourceHandlerRegistry registry, String path, String location) {
        String[] pathPatterns = path.endsWith("/")
                ? new String[]{path.substring(0, path.length() - 1), path, path + "*"}
                : new String[]{path, path + "/", path + "**"};

        log.info("Recognized path patterns = [%s]".formatted(String.join(",", pathPatterns)));

        registry.addResourceHandler(pathPatterns)
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .resourceChain(false)
                .addResolver(new IndexHtmlResolver(environment.getProperty("app.frontend.index-html-path")));
    }
}
