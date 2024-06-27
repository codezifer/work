package de.carsten.spring_react.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import static java.util.Objects.nonNull;

@Configuration
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AppConfig implements WebMvcConfigurer {

    Environment environment;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if(environment.acceptsProfiles(Profiles.of("dev"))) {
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
                ? new String[] {path.substring(0, path.length() - 1), path, path + "*"}
                : new String[] {path, path + "/", path + "**"};

        registry.addResourceHandler(pathPatterns)
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
                        Resource resource = super.resolveResource(request, requestPath, locations, chain);
                        if(nonNull(resource)) return resource;
                        return super.resolveResource(request, "/index.html", locations, chain);
                    }
                });
    }
}
