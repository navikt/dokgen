package no.nav.familie.dokumentgenerator.demo.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateService {
    private Handlebars handlebars;


    public Handlebars getHandlebars() {
        return handlebars;
    }

    public void setHandlebars(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @PostConstruct
    public void loadHandlebarTemplates() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        setHandlebars(new Handlebars(loader));
    }

    public List<String> listAllTemplateNames() throws IOException {
        List<String> templateNames = new ArrayList<>();
        File folder = new ClassPathResource("templates").getFile();
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return null;
        }

        for (File file : listOfFiles) {
            templateNames.add(file.getName());
        }
        return templateNames;
    }

    public Template getTemplate(String templateName) throws IOException {
        return this.getHandlebars().compile(templateName);
    }

    public URL getJsonPath(String templateName) {
        return ClassLoader.getSystemResource("json/" + templateName);
    }

    public JsonNode readJsonFile(URL path) {
        if (path != null) {
            ObjectMapper mapper = new ObjectMapper();

            try {
                return mapper.readTree(new File(path.toURI()));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Could not find JSON file!");
                e.printStackTrace();
            }
        }
        return null;
    }

    public Context getContext(JsonNode model) {
        return Context
                .newBuilder(model)
                .resolver(JsonNodeValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE)
                .build();
    }
}
