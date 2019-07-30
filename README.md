# familie-dokgen-java 

## Krav
- OpenJDK 11
- [Maven](https://maven.apache.org/)

## Kom i gang
Klon repositoriet, navigér til prosjektets mappe som inneholder `pom.xml` og kjør `mvn spring-boot:run` for å starte applikasjonen. 

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


## Bygge og kjøre docker lokalt

```
mvn -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install

docker build -t famile-dokgen .

docker run -p 8080:8080 -d --name famile-dokgen famile-dokgen 

docker stop famile-dokgen; docker rm famile-dokgen
```

