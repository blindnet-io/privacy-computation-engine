#!/bin/sh

docker compose -f docker-compose.yml -f docker-compose.app.yml up -d --wait