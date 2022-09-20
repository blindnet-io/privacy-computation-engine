#!/bin/sh

docker compose -f docker-compose.dev.yml down
docker rmi $(docker images -f reference=pce -q --no-trunc)
