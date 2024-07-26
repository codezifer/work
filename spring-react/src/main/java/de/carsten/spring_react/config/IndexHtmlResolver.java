package de.carsten.spring_react.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IndexHtmlResolver extends PathResourceResolver {

    String indexHtmlPath;

    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        Resource resource = super.resolveResource(request, requestPath, locations, chain);
        if (nonNull(resource)) return resource;
        return super.resolveResource(request, indexHtmlPath, locations, chain);
    }
}
