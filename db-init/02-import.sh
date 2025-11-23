#!/bin/bash
set -e

echo "📦 Starting IMDb dataset import..."

DATA_DIR="/docker-entrypoint-initdb.d/imdb"

import_table() {
  file="$1"
  table="$2"

  if [ ! -f "$DATA_DIR/$file" ]; then
    echo "❌ Missing file: $file — skipping import."
    return
  fi

  echo "📥 Importing $file into $table ..."

  gunzip -c "$DATA_DIR/$file" | \
    psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    -c "\COPY $table FROM STDIN WITH (FORMAT TEXT, DELIMITER E'\t', NULL '\N');"

  echo "✔ Imported $file into $table"
}

import_table "title.basics.tsv.gz"       "title_basics"
import_table "title.akas.tsv.gz"         "title_akas"
import_table "title.principals.tsv.gz"   "title_principals"
import_table "title.crew.tsv.gz"         "title_crew"
import_table "title.episode.tsv.gz"      "title_episode"
import_table "title.ratings.tsv.gz"      "title_ratings"
import_table "name.basics.tsv.gz"        "name_basics"

echo "🎉 IMDb dataset import complete!"
