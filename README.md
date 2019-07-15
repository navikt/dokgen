# familie-dokgen-java 

## Krav
- OpenJDK 11
- PDFGen kjørende lokalt på maskinen som lytter på port 8090

## Kom i gang
- Importer prosjektet som et Maven prosjekt og start DemoApplication

## Endepunkter
`GET /maler`: Henter alle malforslagene som ligger i `resources/templates`

`POST /maler/{templateName}`: Oppdater informasjonen i malen

* Parametre:
    * templateName: Navnet på malen du ønsker å oppdatere
    
    
`GET /maler/markdown/{templateName}`: Henter den genererte markdown handlebars malen.

* Parametre:
    * templateName: Navnet på ønsket mal
    
`GET /maler/html/{templateName}`: Henter den genererte handlebars malen i HTML.

* Parametre:
    * templateName: Navnet på ønsket mal
    
`GET /maler/pdf/{templateName}`: Genererer en PDF/A versjonen av handlebars malen

* Parametre:
    * templateName: Navnet på ønsket generert mal
