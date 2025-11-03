import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProblems } from '../lib/api'

type Problem = {
  id: string
  title: string
  description: string
  difficulty: string
  tables: string[]
  starterSql?: string
}

export default function Problems() {
  const [items, setItems] = useState<Problem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getProblems()
      .then(setItems)
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  const groups = useMemo(() => {
    const order = ['easy','medium','hard'] as const
    const map: Record<string, Problem[]> = { easy: [], medium: [], hard: [] }
    for (const p of items) {
      const k = (p.difficulty || 'easy').toLowerCase()
      if (map[k]) map[k].push(p); else (map as any)[k] = [p]
    }
    return order.map(k => ({ key: k, items: map[k] || [] }))
  }, [items])

  if (loading) return <div className="text-sm text-slate-500">Loading problems...</div>
  if (error) return <div className="text-sm text-red-600">Error: {error}</div>

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Problems</h2>
        <span className="text-sm text-slate-500">{items.length} total</span>
      </div>
      {groups.map(g => (
        <div key={g.key} className="space-y-3">
          <div className="flex items-center gap-2">
            <h3 className="text-lg font-medium capitalize">{g.key}</h3>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800">{g.items.length}</span>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {g.items.map(p => (
              <Link key={p.id} to={`/p/${p.id}`} className="no-underline text-inherit">
                <div className="border border-slate-200 dark:border-slate-700 rounded-lg p-4 h-full bg-white/80 dark:bg-slate-900/60 backdrop-blur-sm hover:border-sky-300 transition-colors">
                  <div className="flex items-center justify-between mb-2">
                    <strong className="font-medium">{p.title}</strong>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 capitalize">{p.difficulty}</span>
                  </div>
                  <p className="m-0 text-sm text-slate-600 dark:text-slate-300">{p.description}</p>
                  <div className="mt-2 text-xs text-slate-500">Tables: {p.tables.join(', ')}</div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
