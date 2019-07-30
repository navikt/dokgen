package no.nav.familie.dokumentgenerator.dokgen.utils;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    private void writeToFile(String folder, String fileName, String content) throws IOException {
//        String tempName = fileName + ".hbs";
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        new File("./content/templates/" + folder + "/" + fileName).getPath()
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
            String fileName = templateName + ".hbs";
            writeToFile(templateName, fileName, strippedHtmlSyntax);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getCss(String cssName){
        try {
            return new String(Files.readAllBytes(Paths.get("./content/assets/css/" + cssName + ".css")));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Kunne ikke åpne template malen");
        }
        return null;
    }

    public String createNewTestSet(String templateName, String testSetName, String testSetContent) {
        String path = "content/templates/" + templateName + "/testdata/" + testSetName + ".json";
        Path newFilePath = Paths.get(path);
        try {
            Files.write(newFilePath, testSetContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            return testSetName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
