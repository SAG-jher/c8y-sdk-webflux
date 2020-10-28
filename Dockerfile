FROM openjdk:8
ADD minefield-1.0-SNAPSHOT.tar /data/
ENTRYPOINT /data/minefield-1.0-SNAPSHOT/bin/minefield