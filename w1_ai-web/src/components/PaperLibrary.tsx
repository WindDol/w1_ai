import { FileText, Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import type { PaperSummary } from '../types'

type Props = {
  papers: PaperSummary[]
  selectedId?: number
  loading: boolean
  onSelect: (paperId: number) => void
}

export function PaperLibrary({ papers, selectedId, loading, onSelect }: Props) {
  const [query, setQuery] = useState('')
  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase()
    return normalized ? papers.filter((paper) => paper.title.toLowerCase().includes(normalized)) : papers
  }, [papers, query])

  return (
    <aside className="library-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">LIBRARY</span>
          <h2>论文库</h2>
        </div>
        <span className="count-badge">{papers.length}</span>
      </div>
      <label className="search-field">
        <Search size={15} />
        <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索论文" />
      </label>
      <div className="paper-list" aria-busy={loading}>
        {loading && <div className="compact-empty">正在读取论文库...</div>}
        {!loading && filtered.map((paper) => (
          <button
            type="button"
            key={paper.id}
            className={`paper-list-item ${selectedId === paper.id ? 'is-selected' : ''}`}
            onClick={() => onSelect(paper.id)}
          >
            <FileText size={16} />
            <span>
              <strong>{paper.title}</strong>
              <small>Paper #{paper.id} · {paper.status || 'COMPLETED'}</small>
            </span>
          </button>
        ))}
        {!loading && !filtered.length && <div className="compact-empty">没有匹配的论文</div>}
      </div>
    </aside>
  )
}
