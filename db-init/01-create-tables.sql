CREATE TABLE IF NOT EXISTS title_basics (
    tconst TEXT,
    titleType TEXT,
    primaryTitle TEXT,
    originalTitle TEXT,
    isAdult TEXT,
    startYear TEXT,
    endYear TEXT,
    runtimeMinutes TEXT,
    genres TEXT
);

CREATE TABLE IF NOT EXISTS title_akas (
    titleId TEXT,
    ordering TEXT,
    title TEXT,
    region TEXT,
    language TEXT,
    types TEXT,
    attributes TEXT,
    isOriginalTitle TEXT
);

CREATE TABLE IF NOT EXISTS title_principals (
    tconst TEXT,
    ordering TEXT,
    nconst TEXT,
    category TEXT,
    job TEXT,
    characters TEXT
);

CREATE TABLE IF NOT EXISTS title_crew (
    tconst TEXT,
    directors TEXT,
    writers TEXT
);

CREATE TABLE IF NOT EXISTS title_episode (
    tconst TEXT,
    parentTconst TEXT,
    seasonNumber TEXT,
    episodeNumber TEXT
);

CREATE TABLE IF NOT EXISTS title_ratings (
    tconst TEXT,
    averageRating TEXT,
    numVotes TEXT
);

CREATE TABLE IF NOT EXISTS name_basics (
    nconst TEXT,
    primaryName TEXT,
    birthYear TEXT,
    deathYear TEXT,
    primaryProfession TEXT,
    knownForTitles TEXT
);
