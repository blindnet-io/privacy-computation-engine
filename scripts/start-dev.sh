#!/bin/sh

source $(dirname "$0")/stop-dev.sh

docker compose -f docker-compose.dev.yml up -d --remove-orphans --wait