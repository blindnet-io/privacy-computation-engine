#!/bin/sh

sbt docker:publishLocal
docker compose -f docker-compose.yml up -d --wait
