package no.nav.familie.dokumentgenerator.demo.utils;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public String getTemplatePath(String templateName) {
        return String.format("./content/templates/%1$s/%1$s.hbs", templateName);
    }

    public List<String> getResourceNames(String path) {
        List<String> resourceNames = new ArrayList<>();
        File folder;
        File[] listOfFiles;
        folder = new File(path);
        listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return null;
        }

        for (File file : listOfFiles) {
            resourceNames.add(FilenameUtils.getBaseName(file.getName()));
        }

        return resourceNames;
    }

    private void writeToFile(String name, String content) throws IOException {
        String tempName = name + ".hbs";
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        new File("./content/templates/" + name + "/" + tempName).getPath()
                )
        );
        writer.append(content);
        writer.close();
    }

    public void saveTemplateFile(String templateName, String markdownContent) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        String strippedHtmlSyntax = Jsoup.clean(
                markdownContent,
                "",
                Whitelist.none(),
                settings
        );

        try {
            writeToFile(templateName, strippedHtmlSyntax);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
