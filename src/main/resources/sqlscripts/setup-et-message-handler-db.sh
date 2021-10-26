#!/usr/bin/env bash
# Creates et_message_handler db for local dev, and its tables and functions that are required by et-message-handler service

echo "Creating et_message_handler database"
psql postgresql://localhost:5050 -v ON_ERROR_STOP=1 -U postgres <<-EOSQL
  CREATE USER et_message_handler WITH PASSWORD 'et_message_handler';

  CREATE DATABASE et_message_handler
    WITH OWNER = et_message_handler
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
EOSQL

set -e

echo "Running tbls_PersistentQ_multiplecounter_v1.0.sql"
psql postgresql://localhost:5050/et_message_handler -U et_message_handler -f ./tbls_PersistentQ_multiplecounter_v1.0.sql

echo "Running tbls_PersistentQ_multipleErrors_v1.1.sql"
psql postgresql://localhost:5050/et_message_handler -U et_message_handler -f ./tbls_PersistentQ_multipleErrors_v1.1.sql

echo "Running fn_persistentQ_getNextMultipleCountVal_v1.0.sql"
psql postgresql://localhost:5050/et_message_handler -U et_message_handler -f ./fn_persistentQ_getNextMultipleCountVal_v1.0.sql

echo "Running fn_persistentQ_logMultipleError_v1.3.sql"
psql postgresql://localhost:5050/et_message_handler -U et_message_handler -f ./fn_persistentQ_logMultipleError_v1.3.sql
