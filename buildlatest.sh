echo "Bygger sykmelding-gateway latest"

./gradlew bootJar

docker build . -t sykmelding-gateway:latest
