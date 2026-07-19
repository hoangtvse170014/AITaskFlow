#!/bin/sh
set -e

echo "=== Railway Environment Debug ==="
echo "PORT=$PORT"
echo "POSTGRES_HOST=$POSTGRES_HOST"
echo "POSTGRES_DB=$POSTGRES_DB"
echo "POSTGRES_USER=$POSTGRES_USER"
echo "DATABASE_URL=${DATABASE_URL:+[SET]}"

# Build JDBC URL from Railway PostgreSQL variables
if [ -n "$POSTGRES_HOST" ]; then
    # Railway provides individual PostgreSQL variables
    JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT:-5432}/${POSTGRES_DB}?sslmode=require"
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    echo "Built DB_URL from POSTGRES_* variables: $JDBC_URL"
elif [ -n "$DATABASE_URL" ]; then
    # Railway provides DATABASE_URL - transform from postgresql:// to jdbc:postgresql://
    JDBC_URL=$(echo "$DATABASE_URL" | sed 's|^postgresql://|jdbc:postgresql://|')
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    echo "Transformed DATABASE_URL to JDBC URL"
    echo "DB_URL=$DB_URL"
fi

# Use Railway PORT, default to 8080
SERVER_PORT=${PORT:-8080}

echo "=== Starting Spring Boot ==="
echo "Server port: $SERVER_PORT"

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -jar /app/app.jar \
    --server.port=$SERVER_PORT
