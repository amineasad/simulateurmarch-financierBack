package tn.esprit.examen.nomPrenomClasseExamen.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PI Finance API")
                        .description("API de trading en temps réel avec WebSocket pour la gestion des ordres, matching automatique et notifications temps réel")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PI Finance Team")
                                .email("contact@pifinance.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:9090/examen")
                                .description("Serveur de développement"),
                        new Server()
                                .url("https://api.pifinance.com")
                                .description("Serveur de production")
                ));
    }
}
