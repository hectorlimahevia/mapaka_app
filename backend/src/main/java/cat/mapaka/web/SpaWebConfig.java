package cat.mapaka.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serveix el frontend Vue empaquetat com a recursos estàtics del mateix jar (Prompt 13 —
 * coste zero, backend i frontend en un únic servei de Render). Vue Router fa servir mode
 * "history" (frontend/src/router), així que qualsevol ruta que no correspongui a un fitxer
 * estàtic real (p. ex. /parent/resum en recarregar la pàgina) ha de servir index.html
 * perquè sigui el router del navegador qui la resolgui, no el servidor.
 *
 * Els controllers de @RestController (/api/**) i Actuator (/actuator/**) es resolen sempre
 * abans que aquest resource handler, perquè Spring dona prioritat a RequestMappingHandlerMapping
 * sobre el SimpleUrlHandlerMapping que registra addResourceHandlers — mai interfereix amb l'API.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        return requested.exists() && requested.isReadable()
                                ? requested
                                : new ClassPathResource("/static/index.html");
                    }
                });
    }
}
