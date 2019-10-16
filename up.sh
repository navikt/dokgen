
docker stop familie-dokgen; docker rm familie-dokgen
mvn -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install
docker build -t navikt/dokgen .
docker run -p 7281:8080 -d --name familie-dokgen navikt/dokgen