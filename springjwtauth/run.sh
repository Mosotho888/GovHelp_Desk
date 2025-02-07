#!/bin/bash

# Set up environment variables from the .env file
set -a
source .env
set +a

# Create the Docker network (if it doesn't exist already)
docker network inspect spring-boot-api-network > /dev/null 2>&1 || \
    docker network create spring-boot-api-network

# Build and start the Docker containers using Docker Compose
#docker-compose -f docker-compose.yml up --build -d
docker build -t tebohogiven/spring-boot-app:0.0.1 -f Dockerfile .

# Output the status of the containers
docker-compose ps

# Optionally, you can view the logs of the services after starting them
# docker-compose logs -f

# Instructions for stopping and removing containers
echo "To stop and remove containers, networks, and volumes, run:"
echo "docker-compose down"
