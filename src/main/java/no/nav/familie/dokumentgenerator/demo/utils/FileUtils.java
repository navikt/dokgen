package no.nav.familie.dokumentgenerator.demo.utils;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileUtils {

    public String getTemplatePath(String templateName) {
        return String.format("templates/%1$s/%1$s.hbs", templateName);
    }

    private static URL getCssPath(String cssName) {
        return ClassLoader.getSystemResource("static/css/" + cssName);
    }

    public List<String> getResourceNames(String path) {
        List<String> resourceNames = new ArrayList<>();
        File folder;
        File[] listOfFiles;
        try {
            folder = new ClassPathResource(path).getFile();
            listOfFiles = folder.listFiles();

            if (listOfFiles == null) {
                return null;
            }

            for (File file : listOfFiles) {
                resourceNames.add(FilenameUtils.getBaseName(file.getName()));
            }

            return resourceNames;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeToFile(String name, String content) throws IOException {
        String tempName = name + ".hbs";
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(
                        ClassLoader.getSystemResource
                                ("templates/" + name + "/" + tempName).getPath(), false));
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

    static String getCssFile(String fileName) {
        URI filePath = null;
        try{
            filePath = getCssPath(fileName).toURI();
        }
        catch (URISyntaxException e){
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        List<String> stringList = new ArrayList<>();
        if(filePath != null){
            try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
                stringList = stream.collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for(String line : stringList){
            sb.append(line);
        }

        return sb.toString();
    }

    public void createNewTestSet(String templateName, String testSetName, String testSetContent) {
        String path = "templates/" + templateName + "/testdata/" + testSetName + ".json";
        Path newFilePath = Paths.get(path);
        try {
            Files.createFile(newFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
