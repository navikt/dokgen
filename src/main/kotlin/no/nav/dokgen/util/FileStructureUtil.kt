package no.nav.dokgen.util

import java.nio.file.Path

object FileStructureUtil {
    private const val DEFAULT_VARIATION = "template"
    fun getTemplateSchemaPath(contentRoot: Path, templateName: String): Path {
        return contentRoot.resolve("templates/$templateName/schema.json")
    }

    fun getTemplatePath(contentRoot: Path, templateName: String): Path {
        return getTemplatePath(contentRoot, templateName, DEFAULT_VARIATION)
    }

    fun getTemplatePath(contentRoot: Path, templateName: String, variation: String): Path {
        return contentRoot.resolve("templates/$templateName/$variation.hbs")
    }

    fun getTestDataPath(contentRoot: Path, templateName: String, testDataName: String): Path {
        return contentRoot.resolve("templates/$templateName/testdata/$testDataName.json")
    }

    fun getTestDataRootPath(contentRoot: Path, templateName: String?): Path {
        return contentRoot.resolve(String.format("templates/%s/testdata/", templateName))
    }

    fun getTemplateRootPath(contentRoot: Path): Path {
        return contentRoot.resolve("templates")
    }

    fun getFormatSchema(contentRoot: Path, format: DocFormat): Path {
        return contentRoot.resolve("formats/" + format + "schema.json")
    }

    fun getFormatHeader(contentRoot: Path, format: DocFormat): Path {
        return contentRoot.resolve("formats/$format/header.html")
    }

    fun getFormatFooter(contentRoot: Path, format: DocFormat): Path {
        return contentRoot.resolve("formats/$format/footer.html")
    }

    fun getCss(contentRoot: Path, format: DocFormat): Path {
        return contentRoot.resolve("formats/$format/style.css")
    }

    fun getFont(contentRoot: Path, fontName: String): Path {
        return contentRoot.resolve("fonts/$fontName")
    }
}