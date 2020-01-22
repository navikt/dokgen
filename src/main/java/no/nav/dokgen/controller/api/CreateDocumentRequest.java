package no.nav.dokgen.controller.api;

import no.nav.dokgen.util.DocFormat;

public class CreateDocumentRequest {
    DocFormat docFormat;
    String templateContent;

    boolean precompiled;
    String mergeFields;

    boolean includeHeader;
    String headerFields;

    public CreateDocumentRequest() {
    }

    public CreateDocumentRequest(DocFormat docFormat,
                                 String templateContent,
                                 boolean precompiled,
                                 String mergeFields,
                                 boolean includeHeader,
                                 String headerFields) {
        this.docFormat = docFormat;
        this.templateContent = templateContent;
        this.precompiled = precompiled;
        this.mergeFields = mergeFields;
        this.includeHeader = includeHeader;
        this.headerFields = headerFields;
    }

    public DocFormat getDocFormat() {
        return docFormat;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public boolean isPrecompiled() {
        return precompiled;
    }

    public String getMergeFields() {
        return mergeFields;
    }

    public boolean isIncludeHeader() {
        return includeHeader;
    }

    public String getHeaderFields() {
        return headerFields;
    }
}
