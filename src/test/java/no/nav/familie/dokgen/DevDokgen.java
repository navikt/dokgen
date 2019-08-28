package no.nav.familie.dokgen;

import no.nav.familie.dokgen.configuration.ApplicationConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class DevDokgen {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ApplicationConfig.class)
                .profiles("dev")
                .run(args);
    }

}
