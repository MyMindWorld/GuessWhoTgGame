#!/usr/bin/env bash

rm *.jar
mvn -f ../ clean package -DskipTests || {
  echo "Jar Build failed"
  exit 2
}
cp ../target/*.jar .
mv *.jar main.jar
docker build --build-arg TOKEN=$GUESS_WHO_TG_TOKEN -t mmw/guess_who_tg_bot:latest . || {
  echo "Docker Build failed"
  exit 2
}
docker-compose down
docker-compose up -d
