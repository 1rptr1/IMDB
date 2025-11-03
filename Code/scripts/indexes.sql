-- Recommended indexes for common joins and filters
CREATE INDEX IF NOT EXISTS idx_akas_titleId ON akas(titleId);
CREATE INDEX IF NOT EXISTS idx_akas_region ON akas(region);
CREATE INDEX IF NOT EXISTS idx_akas_language ON akas(language);

CREATE INDEX IF NOT EXISTS idx_principals_tconst ON principals(tconst);
CREATE INDEX IF NOT EXISTS idx_principals_nconst ON principals(nconst);

CREATE INDEX IF NOT EXISTS idx_ratings_tconst ON ratings(tconst);
CREATE INDEX IF NOT EXISTS idx_crew_tconst ON crew(tconst);

CREATE INDEX IF NOT EXISTS idx_episode_parent ON episode(parentTconst);
