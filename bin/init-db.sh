#!/bin/bash
# Create et_msg_handler database

set -e

if [ -z "$DB_URL" ] || [ -z "ET_MSG_HANDLER_POSTGRES_PASSWORD" ]; then
  echo "ERROR: Missing environment variables. Set value for 'DB_URL' and 'ET_MSG_HANDLER_POSTGRES_PASSWORD'."
  exit 1
fi

psql ${DB_URL} -v ON_ERROR_STOP=1 --username postgres --set USERNAME=et_msg_handler --set PASSWORD=${ET_MSG_HANDLER_POSTGRES_PASSWORD} <<-EOSQL
  CREATE USER :USERNAME WITH PASSWORD ':PASSWORD';
  CREATE DATABASE et_msg_handler
    WITH OWNER = :USERNAME
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
EOSQL
