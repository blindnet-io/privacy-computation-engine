#!/bin/sh

docker compose -f docker-compose.yml down
docker rmi $(docker images -f reference=pce -q --no-trunc)
