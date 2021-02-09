FROM java:8

ARG TOKEN="not_set"
ENV GUESS_WHO_TG_TOKEN $TOKEN

WORKDIR /app
COPY ./ ./

CMD ["java", "-jar", "main.jar"]
