#!/bin/sh
set -e

echo "=========================================="
echo "  Railway Deployment Entry Point"
echo "=========================================="

# Railway sets PORT automatically
echo "Railway PORT: $PORT"

# Railway PostgreSQL provides DATABASE_URL in format: postgresql://user:pass@host:port/db?sslmode=require
# Spring Boot JDBC driver needs: jdbc:postgresql://host:port/db?sslmode=require

echo ""
echo "Checking DATABASE_URL..."
if [ -n "$DATABASE_URL" ]; then
    echo "Found DATABASE_URL, transforming..."
    # Extract host:port/database from postgresql://user:pass@host:port/db
    # Remove 'postgresql://' and everything up to '@'
    # Handle format: postgresql://user:pass@host:port/db?query
    REMAINING="${DATABASE_URL#postgresql://*@}"
    # Now REMAINING is like host:port/db?query
    # Extract hostport (everything before /)
    HOSTPORT="${REMAINING%%/*}"
    # Extract dbname (everything after first /, before ?)
    DBNAME="${REMAINING%%\?*}"
    
    JDBC_URL="jdbc:postgresql://${HOSTPORT}/${DBNAME}"
    
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    
    echo "DB_URL transformed to: $JDBC_URL"
    
    # Extract username/password for DB_USERNAME and DB_PASSWORD
    # Format: postgresql://user:pass@host:port/db
    CREDS="${DATABASE_URL#postgresql://}"
    CREDS="${CREDS%@*}"
    export DB_USERNAME="${CREDS%:*}"
    export DB_PASSWORD="${CREDS#*:}"
    
    echo "DB_USERNAME: $DB_USERNAME"
else
    echo "WARNING: DATABASE_URL not found!"
fi

# Railway also sets individual PostgreSQL variables (Railway v2)
echo ""
echo "Checking POSTGRES_* variables..."
if [ -n "$POSTGRES_HOST" ]; then
    JDBC_URL="jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-taskflow}?sslmode=require"
    export DB_URL="$JDBC_URL"
    echo "DB_URL from POSTGRES_HOST: $JDBC_URL"
fi

if [ -n "$POSTGRES_USER" ]; then
    export DB_USERNAME="$POSTGRES_USER"
    echo "DB_USERNAME: $DB_USERNAME"
fi

if [ -n "$POSTGRES_PASSWORD" ]; then
    export DB_PASSWORD="$POSTGRES_PASSWORD"
    echo "DB_PASSWORD: [SET]"
fi

# Set Spring Profile
export SPRING_PROFILES_ACTIVE="prod"
echo ""
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"

# Server port
SERVER_PORT="${PORT:-8080}"
echo "SERVER_PORT: $SERVER_PORT"

echo ""
echo "=========================================="
echo "  Starting Spring Boot Application"
echo "=========================================="
echo "Final DB_URL: $DB_URL"
echo "Final DB_USERNAME: $DB_USERNAME"
echo ""

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -jar /app/app.jar \
    --server.port=$SERVER_PORT \
    --spring.profiles.active=prod
