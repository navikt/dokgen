package no.nav.familie.dokumentgenerator.dokgen;

import no.nav.familie.dokumentgenerator.dokgen.configuration.ApplicationConfig;
import no.nav.familie.dokumentgenerator.dokgen.configuration.DelayedShutdownHook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class DevDokgen {

    public static void main(String[] args) {
        new SpringApplicationBuilder(ApplicationConfig.class)
                .profiles("dev")
                .run(args);
    }

}
