package de.carsten.spring_react.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Log4j2
public class AppConfig implements WebMvcConfigurer {

    Environment environment;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            serveDirectory(registry, "/dev-dashboard", "classpath:/dev_assets/");
        }
        serveDirectory(registry, "/", "classpath:/static/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*");
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
