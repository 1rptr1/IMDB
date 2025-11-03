const BASE: string = (import.meta as any).env?.VITE_API_URL ?? ''

export async function getProblems() {
  const res = await fetch(`${BASE}/api/problems`)
  if (!res.ok) throw new Error('Failed to fetch problems')
  return res.json()
}

export async function getProblem(id: string) {
  const res = await fetch(`${BASE}/api/problems/${id}`)
  if (!res.ok) throw new Error('Failed to fetch problem')
  return res.json()
}

export async function getHint(id: string) {
  const res = await fetch(`${BASE}/api/problems/${id}/hint`)
  if (!res.ok) throw new Error('Failed to fetch hint')
  return res.json() as Promise<{ verbs: string[] }>
}

export async function getNextProblemId(id: string) {
  const res = await fetch(`${BASE}/api/problems/${id}/next`)
  if (!res.ok) throw new Error('Failed to fetch next id')
  return res.json() as Promise<{ nextId: string | null }>
}

export async function getSchema(tables: string[]) {
  if (!tables || tables.length === 0) return { schemas: {} as Record<string, any[]> }
  const qs = tables.map(t => encodeURIComponent(t)).join(',')
  const res = await fetch(`${BASE}/api/schema?tables=${qs}`)
  if (!res.ok) throw new Error('Failed to fetch schema')
  return res.json() as Promise<{ schemas: Record<string, Array<{name:string,type:string,nullable:boolean}>> }>
}

export async function getSolution(id: string) {
  const res = await fetch(`${BASE}/api/problems/${id}/solution`)
  if (!res.ok) throw new Error('Failed to fetch solution')
  return res.json() as Promise<{ sql: string }>
}

export async function runQuery(sql: string) {
  const res = await fetch(`${BASE}/api/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sql })
  })
  return res.json()
}

export async function grade(problemId: string, sql: string) {
  const res = await fetch(`${BASE}/api/grade`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ problemId, sql })
  })
  return res.json()
}
