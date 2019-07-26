#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username postgres --set USERNAME=camunda --set PASSWORD=camunda <<-EOSQL
  CREATE USER :USERNAME WITH PASSWORD ':PASSWORD';

  CREATE DATABASE camunda
    WITH OWNER = :USERNAME
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
EOSQL
