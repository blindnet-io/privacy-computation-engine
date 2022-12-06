#!/bin/sh

# jdbc connection string
export DB_URI="jdbc:postgresql://localhost:5432/pce"
# database user 
export DB_USER="postgres"
# database user's password
export DB_PASS="mysecretpassword"
# callback api prefix
export APP_CALLBACK_URI="localhost:9000/v0"
# redis
export REDIS_URI="redis://localhost"
# identity
export TOKEN_IDENTITY="token"