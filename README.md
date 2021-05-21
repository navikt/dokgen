# DOKGEN
A document generator for creation of PDFs or html based on markdown templates with handlebars. We think that keeping
the templates in a separate service give our product teams greater flexibility to iterate on the content. We experienced
that often these letters, receipts or confirmation messages is generated at the end of a transaction. This makes it harder
to test and develop.

## Requirements
- OpenJDK 11 and [Maven](https://maven.apache.org/)
- ...or Docker

## Getting started
`Dokgen` is distributed in several ways. This repository contains the server that actually validated input and serve
the templates to the enduser. You can clone/fork the repository and build your own image, or you could simply create
your own repository where you just extend this docker-image. The latest solution is probably the easiest in most 
use-cases.

```Dockerfile
FROM docker.pkg.github.com/navikt/dokgen/dokgen:latest
COPY content content
```

If you want to run it with Java just go to the root folder where `pom.xml` is and run `mvn spring-boot:run` 
to start the application.

### Structure of the content-folder

-  fonts/{fontName}.ttf
-  formats/pdf/style.css
-  formats/pdf/header.html
-  formats/pdf/footer.html
-  formats/html/style.css
-  templates/{templateName}/template.hbs
-  templates/{templateName}/schema.json
-  templates/{templateName}/testdata/{testDataName}.json
-  templates/partial.hbs

## Handlebars

### Partials
To solve sharing of templatecode between templates you could simply use the built in concept of partials in handlebars
this can be done by putting a `.hbs` file at the root folder of the `content/templates`-folder. And can be included
with a simple `{{> footer }}`. 

### Helpers
`jknack/handlebars` is preconfigured with a series of [builtin helpers](https://github.com/jknack/handlebars.java#helpers)
we have added some extra by default.
* `dateFormat`

#### Custom Nav helpers
* `add` which lets you add two numbers inline with `{{add 1 2}}` or more usefully inside a #each loop `{{add @index 3}}`
* `table` which lets you generate tables with a set number of columns from an arbitrary amount of `<td>` elements
  ```handlebars
  {{#table columns=2}}
    <td>Some cell</td>
    <td>More data</td>
    <td>Third cell</td>
    <td>You get the gist</td>
  {{/table}}
  ```
  Which would render two rows with two cells each.
  
  | Some cell  |    More data     |
  |------------|------------------|
  | Third cell | You get the gist |
  (Note that github will render the first row bold, this is not how the helper is implemented)

* `norwegian-date` which lets you turn an ISO8601 (`YYYY-MM-dd`) into the more Norwegian `dd.mm.ÅÅÅÅ`

In future releases hopefully all helpers could conditionally be turned on at runtime.

### Variation (language for instance)
By adding a `variation`-parameter to the urls you can have different variations of the same hbs-files.
Example URL: `template/{templateName}/{variation}/create-pdf-variation`.
The variation-parameter is then mapped to file `templates/{templateName}/{variation}.hbs`. 
This opens up for reusing schemas and testdata for several languages or other variations on the same data.
This is all about how you want to structure your `dokgen`-instance.

### Swagger 
This application uses the standard Swagger setup which can be found at `http://localhost:8080/swagger-ui.html` when
running in test.

### Endpoints
`GET /templates`: Will return all templates with relevant links from `content/templates` for other endpoints please use
the swagger route as api documentation.
    

## Build and run docker locally

`write.access` er satt til `false` under prod for å hindre at maler blir endret på mens systemet er i bruk, dette må settes til `true` i `application.yml` under lokal testing.

```
mvn -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install
docker build -t navikt/dokgen .
docker run -p 8081:8080 -d --name dokgen navikt/dokgen
docker stop dokgen; docker rm dokgen
```

