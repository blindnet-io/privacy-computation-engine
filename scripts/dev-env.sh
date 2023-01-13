#!/bin/sh

# jdbc connection string
export BN_DB_URI="jdbc:postgresql://localhost:5432/pce"
# database user 
export BN_DB_USER="postgres"
# database user's password
export BN_DB_PASS="mysecretpassword"
# callback api prefix
export BN_APP_CALLBACK_URI="localhost:9000/v0"
# redis
export BN_REDIS_URI="redis://localhost"
# identity
export BN_TOKEN_IDENTITY="token"
export BN_IDENTITY_URL="https://stage.identity.devkit.blindnet.io"
export BN_IDENTITY_KEY="Ygmu8rfisYXdSqpLOklexj6Hmegt02MEZ9REPghfUpg="