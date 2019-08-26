package no.nav.familie.dokgen;

import no.nav.familie.dokgen.configuration.ApplicationConfig;
import no.nav.familie.dokgen.configuration.DelayedShutdownHook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Dokgen {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApplicationConfig.class);
        app.setRegisterShutdownHook(false);
        ConfigurableApplicationContext applicationContext = app.run(args);
        Runtime.getRuntime().addShutdownHook(new DelayedShutdownHook(applicationContext));
    }

}
