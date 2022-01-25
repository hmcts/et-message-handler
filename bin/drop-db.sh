#!/bin/bash
# Drop et_msg_handler database

set -e

if [ -z "$DB_URL" ] || [ -z "ET_MSG_HANDLER_POSTGRES_PASSWORD" ]; then
  echo "ERROR: Missing environment variables. Set value for 'DB_URL' and 'ET_MSG_HANDLER_POSTGRES_PASSWORD'."
  exit 1
fi

psql ${DB_URL} -v ON_ERROR_STOP=1 --username postgres --set USERNAME=et_msg_handler --set PASSWORD=${ET_MSG_HANDLER_POSTGRES_PASSWORD} <<-EOSQL
  DROP DATABASE et_msg_handler;
  DROP USER :USERNAME;
EOSQL
