#!/bin/bash

# Set up environment variables from the .env file
set -a
source .env
set +a

# Create the Docker network
docker network create spring-boot-api-network

# Build the Docker image
docker build -t spring-boot-api .

# Run the Docker container
docker run --name spring-boot-api-container -it -p 8080:8080 --network spring-boot-api-network \
    spring-boot-api