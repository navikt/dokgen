
docker stop famile-dokgen; docker rm famile-dokgen
mvn -B -Dfile.encoding=UTF-8 -DinstallAtEnd=true -DdeployAtEnd=true  -DskipTests clean install
docker build -t famile-dokgen .
docker run -p 8080:8080 -d --name famile-dokgen famile-dokgen