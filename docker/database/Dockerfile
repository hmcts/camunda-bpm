FROM postgres:17.6

COPY init-db.sh /docker-entrypoint-initdb.d

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD psql -c 'select 1' -d camunda -U camunda

EXPOSE 5432
