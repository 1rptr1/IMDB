import React from 'react'
import { useEffect, useMemo, useState } from 'react'
import { getGenres, getMovies, getMovie, getActor, searchActors, type MovieLite, type MovieDetails, type ActorDetails, type ActorSearchResult } from '../lib/suggestor'

export default function Suggestor() {
  const [genres, setGenres] = useState<string[]>([])
  const [genre, setGenre] = useState<string>('')
  const [actorQuery, setActorQuery] = useState<string>('')
  const [selectedActorId, setSelectedActorId] = useState<string>('')
  const [actorSuggestions, setActorSuggestions] = useState<ActorSearchResult[]>([])
  const [showActorSuggestions, setShowActorSuggestions] = useState(false)
  const [year, setYear] = useState<string>('')
  const [movies, setMovies] = useState<MovieLite[]>([])
  const [totalCount, setTotalCount] = useState<number>(0)
  const [currentPage, setCurrentPage] = useState<number>(1)
  const [loading, setLoading] = useState(false)
  const [selectedMovie, setSelectedMovie] = useState<MovieDetails | null>(null)
  const [selectedActor, setSelectedActor] = useState<ActorDetails | null>(null)
  const [error, setError] = useState<string>('')

  useEffect(() => {
    getGenres().then(setGenres).catch(() => {})
  }, [])

  // Search actors when query changes
  useEffect(() => {
    if (actorQuery.length < 2) {
      setActorSuggestions([])
      setShowActorSuggestions(false)
      return
    }
    
    const timeoutId = setTimeout(async () => {
      try {
        const res = await searchActors(actorQuery, 10)
        setActorSuggestions(res.actors || [])
        setShowActorSuggestions(true)
      } catch (e) {
        setActorSuggestions([])
      }
    }, 300)
    
    return () => clearTimeout(timeoutId)
  }, [actorQuery])

  async function loadMovies(page: number = 1) {
    setLoading(true); setError('')
    try {
      // Ensure page is a valid positive number
      const validPage = Math.max(1, Math.floor(page) || 1)
      const limit = 7
      const offset = (validPage - 1) * limit
      const res = await getMovies({ genre, actorId: selectedActorId || actorQuery, year, limit, offset })
      setMovies(res.items || [])
      setTotalCount(res.items?.length || 0) // Note: Backend doesn't return total count, so we'll use items length
      setCurrentPage(validPage)
    } catch (e: any) {
      setError(e?.message || 'Failed to load movies')
    } finally { setLoading(false) }
  }

  function selectActor(actor: ActorSearchResult) {
    setSelectedActorId(actor.id)
    setActorQuery(actor.name)
    setShowActorSuggestions(false)
    setActorSuggestions([])
    setCurrentPage(1)
  }

  async function openMovie(id: string) {
    setSelectedActor(null)
    try {
      const m = await getMovie(id)
      setSelectedMovie(m)
    } catch (e) {
      // ignore
    }
  }

  async function openActor(id: string) {
    try {
      const a = await getActor(id)
      setSelectedActor(a)
    } catch (e) {
      // ignore
    }
  }

  const canSearch = useMemo(() => !!genre || !!actorQuery || !!year, [genre, actorQuery, year])

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Movie Suggestor</h2>

      <div className="grid md:grid-cols-4 gap-3 items-end">
        <div>
          <label className="block text-sm mb-1">Genre</label>
          <select value={genre} onChange={e=>setGenre(e.target.value)} className="w-full border rounded-md px-2 py-2 bg-white dark:bg-slate-900">
            <option value="">Any</option>
            {genres.map(g => <option key={g} value={g}>{g}</option>)}
          </select>
        </div>
        <div className="relative">
          <label className="block text-sm mb-1">Actor Name</label>
          <input 
            value={actorQuery} 
            onChange={e=>setActorQuery(e.target.value)} 
            onFocus={() => setShowActorSuggestions(actorSuggestions.length > 0)}
            onBlur={() => setTimeout(() => setShowActorSuggestions(false), 200)}
            placeholder="e.g., Tom Hanks" 
            className="w-full border rounded-md px-2 py-2 bg-white dark:bg-slate-900"
          />
          {showActorSuggestions && actorSuggestions.length > 0 && (
            <div className="absolute z-10 w-full mt-1 bg-white dark:bg-slate-900 border rounded-md shadow-lg max-h-60 overflow-auto">
              {actorSuggestions.map(actor => (
                <button
                  key={actor.id}
                  onClick={() => selectActor(actor)}
                  className="w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-800 border-b last:border-b-0"
                >
                  <div className="font-medium">{actor.name}</div>
                  <div className="text-xs text-slate-500">{actor.id} {actor.birthYear && `• Born: ${actor.birthYear}`}</div>
                </button>
              ))}
            </div>
          )}
        </div>
        <div>
          <label className="block text-sm mb-1">Year</label>
          <input value={year} onChange={e=>setYear(e.target.value)} placeholder="1999" className="w-full border rounded-md px-2 py-2 bg-white dark:bg-slate-900"/>
        </div>
        <div>
          <button onClick={() => loadMovies(1)} disabled={!canSearch || loading} className="w-full h-[38px] rounded-md bg-sky-600 text-white disabled:opacity-50">{loading ? 'Loading...' : 'Search'}</button>
        </div>
      </div>

      {error && <div className="text-sm text-red-600">{error}</div>}

      {/* Results */}
      <div className="grid md:grid-cols-2 gap-4">
        <div>
          <h3 className="font-semibold mb-2">Movies {movies.length > 0 && <span className="text-slate-500">(Page {currentPage}, {movies.length} results)</span>}</h3>
          <div className="divide-y border rounded-md">
            {movies.map(m => (
              <button key={m.id} onClick={()=>openMovie(m.id)} className="w-full text-left p-3 hover:bg-slate-50 dark:hover:bg-slate-800">
                <div className="font-medium">{m.title}</div>
                <div className="text-xs text-slate-600 dark:text-slate-400 flex gap-2">
                  {m.year ?? '—'} • {m.genres ?? '—'} • {m.rating ? `⭐ ${m.rating}` : 'No rating'} {m.votes ? `(${m.votes})` : ''}
                </div>
              </button>
            ))}
            {movies.length === 0 && <div className="p-3 text-sm text-slate-500">No results. Choose a filter and search.</div>}
          </div>
          
          {/* Pagination */}
          {movies.length > 0 && (
            <div className="mt-4 flex items-center justify-between">
              <div className="text-sm text-slate-600 dark:text-slate-400">
                Showing {((currentPage - 1) * 7) + 1} to {Math.min(currentPage * 7, movies.length)} of {movies.length} results
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => loadMovies(Math.max(1, currentPage - 1))}
                  disabled={currentPage === 1 || loading}
                  className="px-3 py-1 text-sm border rounded disabled:opacity-50 hover:bg-slate-50 dark:hover:bg-slate-800"
                >
                  Previous
                </button>
                <span className="px-3 py-1 text-sm border rounded bg-slate-100 dark:bg-slate-800">
                  {currentPage}
                </span>
                <button
                  onClick={() => loadMovies(currentPage + 1)}
                  disabled={movies.length < 7 || loading}
                  className="px-3 py-1 text-sm border rounded disabled:opacity-50 hover:bg-slate-50 dark:hover:bg-slate-800"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="space-y-4">
          {/* Movie details */}
          {selectedMovie && (
            <div className="border rounded-md p-3">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold">{selectedMovie.title}</h3>
                <button className="text-xs text-slate-500 hover:text-slate-800" onClick={()=>setSelectedMovie(null)}>Close</button>
              </div>
              <div className="text-xs text-slate-600 dark:text-slate-400 mb-2">
                {selectedMovie.year ?? '—'} • {selectedMovie.genres ?? '—'} • {selectedMovie.rating ? `⭐ ${selectedMovie.rating}` : 'No rating'} {selectedMovie.votes ? `(${selectedMovie.votes})` : ''}
              </div>
              <div>
                <div className="font-medium mb-2">Actors</div>
                <div className="flex flex-wrap gap-2">
                  {selectedMovie.actors?.map(a => (
                    <button key={a.id} onClick={()=>openActor(a.id)} className="text-sm px-2 py-1 rounded border hover:bg-slate-50 dark:hover:bg-slate-800">
                      {a.name}
                      <span className="text-xs text-slate-500"> ({a.category})</span>
                    </button>
                  ))}
                  {(!selectedMovie.actors || selectedMovie.actors.length === 0) && <div className="text-sm text-slate-500">No cast listed.</div>}
                </div>
              </div>
            </div>
          )}

          {/* Actor details */}
          {selectedActor && (
            <div className="border rounded-md p-3">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold">{selectedActor.name}</h3>
                <button className="text-xs text-slate-500 hover:text-slate-800" onClick={()=>setSelectedActor(null)}>Close</button>
              </div>
              <div className="text-xs text-slate-600 dark:text-slate-400 mb-2">Born: {selectedActor.birthYear ?? '—'}</div>
              <div>
                <div className="font-medium mb-2">Top 10 Films</div>
                <div className="divide-y border rounded-md">
                  {selectedActor.topFilms?.map(f => (
                    <button key={f.id} onClick={()=>openMovie(f.id)} className="w-full text-left p-2 hover:bg-slate-50 dark:hover:bg-slate-800">
                      <div className="font-medium">{f.title}</div>
                      <div className="text-xs text-slate-600 dark:text-slate-400 flex gap-2">
                        {f.year ?? '—'} • {f.rating ? `⭐ ${f.rating}` : 'No rating'} {f.votes ? `(${f.votes})` : ''}
                      </div>
                    </button>
                  ))}
                  {(!selectedActor.topFilms || selectedActor.topFilms.length === 0) && <div className="p-2 text-sm text-slate-500">No films found.</div>}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
