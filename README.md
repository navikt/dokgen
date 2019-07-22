# familie-dokgen-java 

## Krav
- OpenJDK 11
- PDFGen kjørende lokalt på maskinen som lytter på port 8090

## Kom i gang
- Importer prosjektet som et Maven prosjekt og start DemoApplication

## Endepunkter
`GET /mal/alle`: Henter alle malforslagene som ligger i `resources/templates`
    
`GET /mal/{templateName}`: Henter ønsket mal i markdownformat.

* Parametre:
    * templateName: Navnet på ønsket mal
    
`GET /maler/{templateName}/testData`: Henter navn på alle datasett for testing av innflettingsfelter.
* Parametre:
* templateName: Navnet på malen for ønsket testdata
    
`POST /mal/{format}/{templateName}`: Henter det genererte dokumentet i ønsket format.

* Parametre:
    * templateName: Navnet på ønsket mal
    * format: Enten `html` eller `pdf`
* Request body:
    * interleavingFields: Innflettingsfelt i JSON-format
    * markdownContent: Innholdet til markdown-malen (Oppdaterer foreløpig ikke)
    
`PUT /mal/{format}/{templateName}`: Oppdaterer/genererer mal og henter det genererte dokumentet i ønsket format.

* Parametre:
    * templateName: Navnet på ønsket mal
    * format: Enten `html` eller `pdf`
* Request body:
    * interleavingFields: Innflettingsfelt i JSON-format
    * markdownContent: Innholdet til markdown-malen

`POST /brev/{format}/{templateName}`: Henter den genererte dokumentet i ønsket format.

* Parametre:
    * templateName: Navnet på ønsket mal
    * format: Enten `html` eller `put`
* Request body:
    * interleavingFields: Innflettingsfelt i JSON-format
    * markdownContent: Innholdet til markdown-malen (Vil fjernes)
