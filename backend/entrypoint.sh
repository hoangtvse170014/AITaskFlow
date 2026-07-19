#!/bin/sh
set -e

echo "=== Railway Environment Debug ==="
echo "PORT=$PORT"
echo "DATABASE_URL=${DATABASE_URL:0:50}..."  # First 50 chars only for security

# Transform Railway DATABASE_URL (postgresql://) to JDBC URL (jdbc:postgresql://)
if [ -n "$DATABASE_URL" ]; then
    # Remove 'postgresql://' prefix and add 'jdbc:postgresql://' prefix
    # Extract host/db from postgresql://user:pass@host:port/db?sslmode=require
    JDBC_URL=$(echo "$DATABASE_URL" | sed 's|^postgresql://|jdbc:postgresql://|')
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    echo "DB_URL=$DB_URL"
fi

# Set default port
SERVER_PORT=${PORT:-8080}

echo "=== Starting Spring Boot ==="
echo "Server port: $SERVER_PORT"

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -jar /app/app.jar \
    --server.port=$SERVER_PORT
