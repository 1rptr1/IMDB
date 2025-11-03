import React, { useEffect, useState } from 'react'
import { Route, Routes, Link, useLocation } from 'react-router-dom'
import Problems from './pages/Problems'
import Problem from './pages/Problem'
import Suggestor from './pages/Suggestor'

export default function App() {
  const [theme, setTheme] = useState<string>(() => localStorage.getItem('theme') || 'light')
  const loc = useLocation()

  useEffect(() => {
    const root = document.documentElement
    if (theme === 'dark') root.classList.add('dark'); else root.classList.remove('dark')
    localStorage.setItem('theme', theme)
  }, [theme])

  return (
    <div className="max-w-6xl mx-auto p-4">
      <header className="flex items-center justify-between mb-4">
        <h1 className="m-0 text-lg font-semibold"><Link to="/">IMDb Tools</Link></h1>
        <nav className="flex items-center gap-2">
          <button onClick={()=>setTheme(theme === 'dark' ? 'light' : 'dark')} className="px-3 py-2 rounded-md bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-sm">
            {theme === 'dark' ? 'Light' : 'Dark'} mode
          </button>
        </nav>
      </header>
      {/* Tabs */}
      <div className="mb-4 border-b border-slate-200 dark:border-slate-800">
        <nav className="flex gap-2">
          <Link to="/" className={`px-3 py-2 text-sm -mb-px border-b-2 ${loc.pathname === '/' || loc.pathname.startsWith('/p/') ? 'border-sky-600 text-sky-600' : 'border-transparent text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'}`}>IMDb SQL Practice</Link>
          <Link to="/suggestor" className={`px-3 py-2 text-sm -mb-px border-b-2 ${loc.pathname === '/suggestor' ? 'border-sky-600 text-sky-600' : 'border-transparent text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white'}`}>Movie Suggestor</Link>
        </nav>
      </div>
      <Routes>
        <Route path="/" element={<Problems/>} />
        <Route path="/p/:id" element={<Problem/>} />
        <Route path="/suggestor" element={<Suggestor/>} />
      </Routes>
    </div>
  )
}
