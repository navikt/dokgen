package no.nav.dokgen.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.nio.file.Path


@Configuration
@ConfigurationProperties(prefix = "path.content")
class ContentProperties {
    var root: Path = Path.of("./content/")
}