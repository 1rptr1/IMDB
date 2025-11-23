#!/bin/bash
set -e

echo "⏳ Waiting for database data import to complete..."

DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-imdb}"
DB_USER="${DB_USER:-imdb}"

# Wait for database to be ready
until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER"; do
  echo "Waiting for database to be ready..."
  sleep 2
done

# Wait for data import to complete (check for tables)
echo "📊 Checking if data import is complete..."
until psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'title_basics';" | grep -q "^[1-9]"; do
  echo "Waiting for data import to complete..."
  sleep 5
done

# Wait for at least some data to be imported
echo "📥 Waiting for data to be imported..."
until psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT COUNT(*) FROM title_basics LIMIT 1;" | grep -q "^[1-9]"; do
  echo "Waiting for data import to start..."
  sleep 10
done

echo "✅ Database is ready with imported data!"

# Show some stats
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT 
  'title_basics' as table_name, COUNT(*) as row_count FROM title_basics
UNION ALL
SELECT 
  'name_basics' as table_name, COUNT(*) as row_count FROM name_basics
UNION ALL  
SELECT 
  'title_ratings' as table_name, COUNT(*) as row_count FROM title_ratings
ORDER BY row_count DESC;
" 2>/dev/null || echo "Data import still in progress..."

echo "🎉 Database is fully ready for connections!"
