#!/bin/bash

docker stop spring-boot-api-container

docker rm spring-boot-api-container

docker network rm spring-boot-api-network