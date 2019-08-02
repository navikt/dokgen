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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@Service
public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    private static FileUtils single_instance = null;
    private final String contentRoot;


    private FileUtils(){
        this.contentRoot = "./content/";
    }

    private FileUtils(String contentRoot){
        this.contentRoot = contentRoot;
    }

    public static FileUtils getInstance() {
        if (single_instance == null)
            single_instance = new FileUtils();

        return single_instance;
    }

    public static FileUtils getInstance(String contentRoot) {
        if (single_instance == null)
            single_instance = new FileUtils(contentRoot);

        return single_instance;
    }

    public String getContentRoot() {
        return contentRoot;
    }

    public String getTemplatePath(String templateName) {
        return String.format(this.getContentRoot() + "templates/%1$s/%1$s.hbs", templateName);
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
                        new File(this.getContentRoot() + "templates/" + name + "/" + tempName).getPath()
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

    String getCss(String cssName){
        try {
            return new String(Files.readAllBytes(Paths.get(this.getContentRoot() + "assets/css/" + cssName + ".css")));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Kunne ikke åpne template malen");
        }
        return null;
    }
}
