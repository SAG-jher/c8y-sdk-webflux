#!/usr/bin/env sh
./gradlew build
rm -R build/docker| true
mkdir build/docker | true
cp Dockerfile build/docker
cp build/distributions/*tar build/docker
docker build build/docker -t minefield:latest

rm -R build/ms |true
mkdir build/ms | true
docker save --output build/ms/image.tar minefield:latest
cp cumulocity.json build/ms/
cd build/ms/
zip c8ytest-1.0.0-SNAPSHOT.zip cumulocity.json image.tar