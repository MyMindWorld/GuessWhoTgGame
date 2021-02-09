#!/usr/bin/env bash

rm *.jar
mvn -f ../ clean package || {
  echo "Jar Build failed"
  exit 2
}
cp ../target/*.jar .
mv *.jar main.jar
docker build --build-arg TOKEN=$GUESS_WHO_TG_TOKEN -t mmw/guess_who_tg_bot:latest . || {
  echo "Docker Build failed"
  exit 2
}
docker stop guess_who_tg_bot
docker run --rm --name "guess_who_tg_bot" -d mmw/guess_who_tg_bot:latest || {
  echo "Run failed"
  exit 2
}
