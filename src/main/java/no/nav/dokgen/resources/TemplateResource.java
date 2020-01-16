package no.nav.dokgen.resources;


public class TemplateResource {

    public String name;

    public String content;

    public String compiledContent;

    public TemplateResource(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
