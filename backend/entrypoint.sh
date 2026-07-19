#!/bin/sh
# Transform Railway DATABASE_URL (postgresql://) to JDBC URL (jdbc:postgresql://)

if [ -n "$DATABASE_URL" ]; then
    # Convert postgresql://user:pass@host:port/db?options to jdbc:postgresql://host:port/db
    JDBC_URL=$(echo "$DATABASE_URL" | sed -E 's|^postgresql://||' | sed -E 's|^(.*@)?([^/]+)|jdbc:postgresql://\2|')
    export DB_URL="$JDBC_URL"
    export SPRING_DATASOURCE_URL="$JDBC_URL"
    echo "Transformed DATABASE_URL to JDBC URL: $JDBC_URL"
fi

# Use PORT from Railway, default to 8080
SERVER_PORT=${PORT:-8080}

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
    -jar app.jar \
    --server.port=$SERVER_PORT
