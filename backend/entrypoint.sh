#!/bin/sh
set -e

echo "=== Railway Environment Check ==="
echo "PORT=$PORT"
echo "DATABASE_URL=[${DATABASE_URL:+SET}]"

# Railway PostgreSQL plugin provides: DATABASE_URL=postgresql://user:pass@host/db?sslmode=require
# Spring needs: jdbc:postgresql://host:port/db?sslmode=require

if [ -n "$DATABASE_URL" ]; then
    # Remove 'postgresql://' prefix manually using case statement
    case "$DATABASE_URL" in
        postgresql://*)
            # Extract everything after 'postgresql://'
            REMAINING="${DATABASE_URL#postgresql://}"
            # Extract host part (everything up to the next /)
            HOSTPART="${REMAINING%%/*}"
            # Extract database part (everything after first /)
            DBPART="${REMAINING#*/}"
            # Build JDBC URL
            JDBC_URL="jdbc:postgresql://${HOSTPART}/${DBPART}"
            export DB_URL="$JDBC_URL"
            export SPRING_DATASOURCE_URL="$JDBC_URL"
            echo "DB_URL set to: $JDBC_URL"
            ;;
        jdbc:postgresql://*)
            export DB_URL="$DATABASE_URL"
            export SPRING_DATASOURCE_URL="$DATABASE_URL"
            echo "DB_URL already JDBC format"
            ;;
        *)
            echo "Unknown DATABASE_URL format: $DATABASE_URL"
            ;;
    esac
fi

# Also support Railway's individual POSTGRES_* variables
if [ -n "$POSTGRES_HOST" ]; then
    JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-taskflow}?sslmode=require"
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    echo "DB_URL built from POSTGRES_*: $JDBC_URL"
fi

# Set username and password from Railway variables
if [ -n "$POSTGRES_USER" ]; then
    export DB_USERNAME="$POSTGRES_USER"
fi
if [ -n "$POSTGRES_PASSWORD" ]; then
    export DB_PASSWORD="$POSTGRES_PASSWORD"
fi

# Railway PORT
SERVER_PORT=${PORT:-8080}
export SERVER_PORT

echo "=== Starting Spring Boot on port $SERVER_PORT ==="

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -jar /app/app.jar \
    --server.port=$SERVER_PORT
