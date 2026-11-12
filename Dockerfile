# ---------- Étape 1 : compilation avec Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Les dépendances sont téléchargées dans une couche séparée (mise en cache entre deux déploiements)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Les tests tournent dans le pipeline CI (ils ont besoin de Docker pour Testcontainers)
RUN mvn -B -q package -DskipTests

# ---------- Étape 2 : image d'exécution légère ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# L'application ne tourne pas en root (bonne pratique de sécurité)
RUN useradd --system --create-home appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
# Offre gratuite Render : 512 Mo de RAM -> on limite la mémoire de la JVM
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Duser.timezone=Europe/Paris"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
