const BASE_SUG: string = (import.meta as any).env?.VITE_SUGGESTOR_URL ?? ''
const PREFIX = BASE_SUG && BASE_SUG.trim().length > 0 ? BASE_SUG : '/suggestor'

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${PREFIX}${path}`, init)
  if (!res.ok) throw new Error(`Request failed: ${res.status}`)
  return res.json() as Promise<T>
}

export async function getHealth() {
  return http<{ status: string }>(`/api/health`)
}

export async function getGenres() {
  const data = await http<{ genres: string[] }>(`/api/genres`)
  // handle both array of strings or array of objects with name
  const genres: string[] = Array.isArray(data.genres)
    ? data.genres.map((g: any) => (typeof g === 'string' ? g : g?.name)).filter(Boolean)
    : []
  return genres
}

export type MovieLite = { id: string; title: string; year?: number; genres?: string; rating?: number; votes?: number }
export async function getMovies(params: { genre?: string; actorId?: string; year?: string; limit?: number; offset?: number }) {
  const q = new URLSearchParams()
  if (params.genre) q.set('genre', params.genre)
  if (params.actorId) q.set('actorId', params.actorId)
  if (params.year) q.set('year', params.year)
  if (params.limit != null) q.set('limit', String(params.limit))
  if (params.offset != null) q.set('offset', String(params.offset))
  return http<{ items: MovieLite[] }>(`/api/movies?${q.toString()}`)
}

export type MovieDetails = MovieLite & { actors: Array<{ id: string; name: string; category: string }> }
export async function getMovie(id: string) {
  return http<MovieDetails>(`/api/movies/${encodeURIComponent(id)}`)
}

export type ActorDetails = { id: string; name: string; birthYear?: number; topFilms: Array<{ id: string; title: string; year?: number; rating?: number; votes?: number; score?: number }> }
export async function getActor(id: string) {
  return http<ActorDetails>(`/api/actors/${encodeURIComponent(id)}`)
}
