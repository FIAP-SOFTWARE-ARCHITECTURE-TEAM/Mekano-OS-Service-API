# Mekano OS Service — Makefile
MAVEN := ./mvnw
DOCKER_COMP := docker compose

.PHONY: up down reset-db logs dev test test-domain test-app test-infra test-rest compile clean build docker-build

up:
	$(DOCKER_COMP) up -d

down:
	$(DOCKER_COMP) down

reset-db:
	$(DOCKER_COMP) down -v
	$(DOCKER_COMP) up -d

logs:
	$(DOCKER_COMP) logs -f

dev:
	$(MAVEN) quarkus:dev -pl os-rest -am

test:
	$(MAVEN) test

test-domain:
	$(MAVEN) test -pl os-domain

test-app:
	$(MAVEN) test -pl os-application -am

test-infra:
	$(MAVEN) test -pl os-infrastructure -am

test-rest:
	$(MAVEN) test -pl os-rest -am

compile:
	$(MAVEN) compile

clean:
	$(MAVEN) clean

build:
	$(MAVEN) verify -pl os-rest -am

docker-build:
	docker build -f os-rest/src/main/docker/Dockerfile.jvm -t mekano-os-service:latest .
