
docker stop familie-dokgen; docker rm familie-dokgen
mvn -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install
docker build -t familie-dokgen .
docker run -p 8080:8080 -d --name familie-dokgen familie-dokgen