#!/bin/sh

set -e

# Base URL of the configuration API on the PCE instance to configure
BASE_URL="http://localhost:9000/v0"

# data categories

echo "== Selectors =="
echo

curl -v --location --request PUT \
  --url "${BASE_URL}/configure/selectors" \
  --header 'Content-Type: application/json' \
  --data-raw '[
  { "name": "PROOF", "data_category": "OTHER-DATA" }
]'

echo

# general info

echo "== General information =="
echo

curl -v --request PUT \
  --url "${BASE_URL}/configure/general-info" \
  --header 'Content-Type: application/json' \
  --data '{
  "countries": [
    "France",
    "USA"
  ],
  "organization": "blindnet",
  "dpo": "Vuk Janosevic, www.blindnet.io/privacy-request-builder",
  "data_consumer_categories": [
    "Blindnet account managers",
    "Blindnet'\''s DPO"
  ],
  "privacy_policy_link": "https://blindnet.io/privacy",
  "data_security_info": "We use administrative, technical, and physical safeguards to protect your personal data, taking into account the nature of the personal data and the processing, and the threats posed."
}'

echo

# provenances

echo "== Provenances =="
echo

curl -v --request PUT \
  --url "${BASE_URL}/configure/provenances" \
  --header 'Content-Type: application/json' \
  --data '[
	{
    "data_category": "*",
    "provenance": "USER",
    "system": "demo"
  }
]'

echo

# retention policies

echo "== Retention Policies =="
echo

curl -v --request PUT \
  --url "${BASE_URL}/configure/retention-policies" \
  --header 'Content-Type: application/json' \
  --data '[
  {
    "data_category": "*",
    "policy": "NO-LONGER-THAN",
    "duration": "P10D",
    "after": "RELATIONSHIP-END"
  }
]'

echo

# legal bases

echo "== Legal Bases =="
echo

curl -v --location --request PUT \
  --url "${BASE_URL}/configure/legal-bases" \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "lb_type": "CONSENT",
    "name": "Prizes consent",
    "description": "",
    "scope": [
        { "dc": "CONTACT.EMAIL", "pc": "*", "pp": "*" },
        { "dc": "NAME", "pc": "*", "pp": "*" },
        { "dc": "UID.ID", "pc": "*", "pp": "*" },
        { "dc": "OTHER-DATA.PROOF", "pc": "*", "pp": "*" }
    ]
}'

echo
