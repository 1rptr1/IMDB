import React from 'react'
import { useEffect, useMemo, useState } from 'react'
import { getGenres, getMovies, getMovie, getActor, type MovieLite, type MovieDetails, type ActorDetails } from '../lib/suggestor'

export default function Suggestor() {
  const [genres, setGenres] = useState<string[]>([])
  const [genre, setGenre] = useState<string>('')
  const [actorId, setActorId] = useState<string>('')
  const [year, setYear] = useState<string>('')
  const [movies, setMovies] = useState<MovieLite[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedMovie, setSelectedMovie] = useState<MovieDetails | null>(null)
  const [selectedActor, setSelectedActor] = useState<ActorDetails | null>(null)
  const [error, setError] = useState<string>('')

  useEffect(() => {
    getGenres().then(setGenres).catch(() => {})
  }, [])

  async function loadMovies() {
    setLoading(true); setError('')
    try {
      const res = await getMovies({ genre, actorId, year, limit: 50, offset: 0 })
      setMovies(res.items || [])
    } catch (e: any) {
      setError(e?.message || 'Failed to load movies')
    } finally { setLoading(false) }
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

  const canSearch = useMemo(() => !!genre || !!actorId || !!year, [genre, actorId, year])

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
        <div>
          <label className="block text-sm mb-1">Actor ID (nconst)</label>
          <input value={actorId} onChange={e=>setActorId(e.target.value)} placeholder="nm0000151" className="w-full border rounded-md px-2 py-2 bg-white dark:bg-slate-900"/>
        </div>
        <div>
          <label className="block text-sm mb-1">Year</label>
          <input value={year} onChange={e=>setYear(e.target.value)} placeholder="1999" className="w-full border rounded-md px-2 py-2 bg-white dark:bg-slate-900"/>
        </div>
        <div>
          <button onClick={loadMovies} disabled={!canSearch || loading} className="w-full h-[38px] rounded-md bg-sky-600 text-white disabled:opacity-50">{loading ? 'Loading...' : 'Search'}</button>
        </div>
      </div>

      {error && <div className="text-sm text-red-600">{error}</div>}

      {/* Results */}
      <div className="grid md:grid-cols-2 gap-4">
        <div>
          <h3 className="font-semibold mb-2">Movies {movies.length > 0 && <span className="text-slate-500">({movies.length})</span>}</h3>
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
