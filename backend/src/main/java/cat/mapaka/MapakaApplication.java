package cat.mapaka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration exclosa: l'autenticació és manual via JWT
// (cat.mapaka.auth.AuthService), no fem servir el UserDetailsService estàndard d'Spring Security.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MapakaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MapakaApplication.class, args);
	}

}
