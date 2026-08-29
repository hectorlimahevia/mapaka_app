package cat.mapaka.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Neon i Render proporcionen DATABASE_URL amb l'esquema postgres://usuari:contrasenya@host/bd
 * (o postgresql://) — el driver JDBC de PostgreSQL NO accepta credencials incrustades a
 * l'autoritat de la URL (només com a paràmetres de consulta o com a
 * spring.datasource.username/password separats), així que cal partir-les abans que
 * DataSourceAutoConfiguration/Flyway les facin servir (ambdós llegeixen del mateix
 * DataSource configurat, no calen propietats de Flyway a part).
 *
 * És l'única font de spring.datasource.* en producció (application-prod.properties no en
 * defineix cap): si DATABASE_URL ja ve com jdbc:postgresql://... es fa servir tal qual;
 * si ve com postgres(ql)://usuari:contrasenya@host/bd es parteix. Calia que fos l'única
 * font perquè un intent anterior de deixar-ho també a application-prod.properties (amb
 * ${DATABASE_URL} sense processar) competia en precedència amb aquest post-processor i
 * guanyava el valor cru, sense partir — Flyway rebia "url must start with jdbc".
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null) {
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        if (databaseUrl.startsWith("jdbc:")) {
            props.put("spring.datasource.url", databaseUrl);
        } else {
            URI uri = URI.create(databaseUrl);
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
            props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath() + query);
            if (uri.getUserInfo() != null) {
                String[] userInfo = uri.getUserInfo().split(":", 2);
                props.put("spring.datasource.username", userInfo[0]);
                if (userInfo.length > 1) {
                    props.put("spring.datasource.password", userInfo[1]);
                }
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }
}
