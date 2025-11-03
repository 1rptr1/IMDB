import React, { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useParams } from 'react-router-dom'
import { getProblem, runQuery, grade, getHint, getNextProblemId, getSchema, getSolution } from '../lib/api'

type Problem = {
  id: string
  title: string
  description: string
  difficulty: string
  tables: string[]
  starterSql?: string
}

export default function ProblemPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const [problem, setProblem] = useState<Problem | null>(null)
  const [sql, setSql] = useState('')
  const [rows, setRows] = useState<any[] | null>(null)
  const [grading, setGrading] = useState<{correct?: boolean, expectedCount?: number, actualCount?: number} | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [hint, setHint] = useState<string[] | null>(null)
  const [hintLoading, setHintLoading] = useState(false)
  const [nextId, setNextId] = useState<string | null>(null)
  const [schemas, setSchemas] = useState<Record<string, Array<{name:string,type:string,nullable:boolean}>>>({})
  const [solutionSql, setSolutionSql] = useState<string | null>(null)
  const [solutionLoading, setSolutionLoading] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setRows(null)
    setGrading(null)
    setHint(null)
    getProblem(id)
      .then(async (p: Problem) => {
        setProblem(p)
        setSql('')
        try {
          const nxt = await getNextProblemId(id)
          setNextId(nxt.nextId || null)
        } catch {}
        try {
          const sch = await getSchema(p.tables)
          setSchemas(sch.schemas || {})
        } catch {}
      })
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id])

  // keyboard shortcuts (must be declared before any early returns)
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'enter') {
        e.preventDefault(); run();
      }
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'q') {
        e.preventDefault(); setSql(formatSql(sql));
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [sql])

  const columns = useMemo(() => {
    const cols = rows && rows[0] ? Object.keys(rows[0]) : []
    const hidden = new Set(['tconst','parentTconst','titleId'])
    return cols.filter(c => !hidden.has(c))
  }, [rows])

  const hasSql = (sql || '').trim().length > 0

  if (loading) return <div className="text-sm text-slate-500">Loading...</div>
  if (!problem) return <div className="text-sm text-slate-500">Problem not found</div>

  const run = async () => {
    if (!hasSql) { setError('Please enter a SELECT query.'); return }
    setError(null)
    setRows(null)
    const res = await runQuery(sql)
    if (res.error) setError(res.error)
    else setRows(res.rows)
  }

  const doGrade = async () => {
    if (!hasSql) { setError('Please enter a SELECT query before grading.'); return }
    setError(null)
    setGrading(null)
    const res = await grade(problem!.id, sql)
    if (res.error) setError(res.error)
    else setGrading(res)
  }

  

  return (
    <div className="grid grid-cols-12 gap-4">
      {/* Main editor and results */}
      <main className="col-span-12 lg:col-span-9 space-y-4">
        <div className="space-y-1">
          <h2 className="text-xl font-semibold">{problem.title}</h2>
          <p className="text-sm text-slate-600 dark:text-slate-300">{problem.description}</p>
          <div className="flex items-center gap-3 text-sm">
            <span className="px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 capitalize">{problem.difficulty}</span>
            <span className="text-slate-500">Tables: {problem.tables.join(', ')}</span>
          </div>
        </div>

        {/* Toolbar */}
        <div className="flex items-center justify-between">
          <div className="flex gap-2">
            <button onClick={run} disabled={!hasSql} title={!hasSql? 'Enter SQL to run':''} className={`px-3 py-2 rounded-md text-sm ${hasSql? 'bg-sky-600 hover:bg-sky-700 text-white' : 'bg-slate-300 dark:bg-slate-700 text-slate-600 cursor-not-allowed'}`}>Run</button>
            <button onClick={doGrade} disabled={!hasSql} title={!hasSql? 'Enter SQL to check':''} className={`px-3 py-2 rounded-md text-sm ${hasSql? 'bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600' : 'bg-slate-300 dark:bg-slate-700 text-slate-600 cursor-not-allowed'}`}>Check Answer</button>
            <button onClick={()=>setSql(formatSql(sql))} className="px-3 py-2 rounded-md bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-sm">Auto-format</button>
            <button
              onClick={async ()=>{ 
                if (!id) return; 
                if (hint && hint.length) { setHint(null); return; }
                setHintLoading(true); setError(null); 
                try { const res = await getHint(id); setHint(res.verbs || []); } 
                catch (e:any) { setError(String(e)); } 
                finally { setHintLoading(false); } 
              }}
              className="px-3 py-2 rounded-md bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-sm"
            >{hintLoading ? 'Getting hint...' : (hint && hint.length ? 'Hide Hint' : 'Hint')}</button>
            <button
              onClick={async ()=>{ if (!id) return; setSolutionLoading(true); setError(null); try { const res = await getSolution(id); setSolutionSql(res.sql || ''); } catch (e:any) { setError(String(e)); } finally { setSolutionLoading(false); } }}
              className="px-3 py-2 rounded-md bg-amber-200 hover:bg-amber-300 dark:bg-amber-700 dark:hover:bg-amber-600 text-sm"
            >{solutionLoading ? 'Loading solution...' : 'Show Solution'}</button>
          </div>
          <div className="text-xs text-slate-500">Ctrl+Enter: Run • Ctrl+Q: Format</div>
        </div>

        {/* Reserved space for Hint (prevents layout shift) */}
        <div className="mt-2">
          <div className={`min-h-[56px] text-sm transition-opacity ${hint && hint.length ? 'opacity-100' : 'opacity-0'}`}>
            {hint && hint.length ? (
              <div className="space-y-1">
                <strong className="text-sm">Hint</strong>
                <div className="text-sm text-slate-700 dark:text-slate-200">Verbs used: {hint.join(', ')}</div>
              </div>
            ) : null}
          </div>
        </div>

        <textarea
          value={sql}
          onChange={e=>setSql(e.target.value)}
          placeholder="Write your SQL here..."
          className="w-full h-64 font-mono text-sm p-3 border border-slate-200 dark:border-slate-700 rounded-md bg-white dark:bg-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500"
        />

        {/* Output */}
        <div className="space-y-2">
          <strong className="text-sm">Results {rows ? `(${rows.length})` : ''}</strong>
          {!rows && !error && (
            <div className="text-sm text-slate-500 border border-dashed border-slate-300 dark:border-slate-600 rounded-md p-6 text-center">
              Query results will appear here. Click Run to execute the query.
            </div>
          )}
          {error && <div className="text-red-600 text-sm">Error: {error}</div>}
          {rows && (
            <div className="overflow-auto max-h-[60vh] border border-slate-200 dark:border-slate-700 rounded-md">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0">
                  <tr>
                    {columns.map(c => <th key={c} className="text-left px-3 py-2 border-b border-slate-200 dark:border-slate-700 font-medium">{c}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {rows.slice(0,200).map((r,i) => (
                    <tr key={i} className={i % 2 === 0 ? 'bg-white dark:bg-slate-900' : 'bg-slate-50 dark:bg-slate-800'}>
                      {columns.map(c => <td key={c} className="px-3 py-2 border-b border-slate-100 dark:border-slate-800">{String(r[c])}</td>)}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {grading && (
          <div className="space-y-2">
            <strong className="text-sm">Grading</strong>
            <div className="text-sm">
              {grading.correct ? (
                <span className="text-green-600">Correct!</span>
              ) : (
                <span className="text-amber-600">Not correct. Expected {grading.expectedCount}, got {grading.actualCount}.</span>
              )}
            </div>
          </div>
        )}

        {/* Expected Solution (below editor) */}
        {solutionSql !== null && (
          <div className="space-y-2">
            <strong className="text-sm">Expected Solution</strong>
            <pre className="text-xs whitespace-pre-wrap border border-slate-200 dark:border-slate-700 rounded-md p-3 bg-slate-50 dark:bg-slate-800">{solutionSql}</pre>
          </div>
        )}

        {/* Columns (Quick View) at bottom */}
        <div className="space-y-2">
          <strong className="text-sm">Columns (Quick View)</strong>
          <div className="grid gap-3 md:grid-cols-2">
            {problem.tables.map(t => (
              <div key={t} className="border border-slate-200 dark:border-slate-700 rounded-md p-3">
                <div className="text-sm font-medium mb-1">{t}</div>
                <ul className="text-xs list-disc list-inside max-h-40 overflow-auto">
                  {(schemas[t] || []).map(c => (
                    <li key={c.name} className="truncate">{c.name}</li>
                  ))}
                  {(!schemas[t] || schemas[t].length===0) && (
                    <li className="text-slate-500">No schema info</li>
                  )}
                </ul>
              </div>
            ))}
          </div>
        </div>

        {/* Learning Resources at bottom */}
        <div className="space-y-2">
          <strong className="text-sm">Learning Resources</strong>
          <div className="text-sm text-slate-600 dark:text-slate-300 space-y-1">
            <a className="underline" href="https://www.postgresql.org/docs/current/sql.html" target="_blank">PostgreSQL SQL</a>
            <div>Keybinds: Ctrl+Enter (run), Ctrl+Q (format)</div>
          </div>
        </div>
      </main>

      {/* Right sidebar */}
      <aside className="col-span-12 lg:col-span-3 space-y-3">
        <div className="border border-slate-200 dark:border-slate-700 rounded-md bg-white/70 dark:bg-slate-900/50 backdrop-blur-sm p-3 space-y-2">
          <Link to="/" className="w-full block">
            <button className="w-full px-3 py-2 rounded-md bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-sm">View All Questions</button>
          </Link>
          <button onClick={()=> nextId && nav(`/p/${nextId}`)} disabled={!nextId} className={`w-full px-3 py-2 rounded-md text-sm ${nextId? 'bg-emerald-600 hover:bg-emerald-700 text-white':'bg-slate-300 dark:bg-slate-700 text-slate-600 cursor-not-allowed'}`}>Next Question</button>
        </div>
      </aside>
    </div>
  )
}

function formatSql(s: string) {
  // simple best-effort formatting: trim, collapse multiple spaces, ensure single spaces around commas
  let x = (s || '').trim()
  x = x.replace(/\s+/g, ' ')
  x = x.replace(/\s*,\s*/g, ', ')
  return x
}
