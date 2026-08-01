import { BookOpen, Braces, ExternalLink } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import type { PaperWorkspace, SectionWorkspace } from '../types'

type Props = {
  paper?: PaperWorkspace
  section?: SectionWorkspace
  loading: boolean
  highlightedQuote?: string
  onSelectText?: (text: string) => void
}

export function Reader({ paper, section, loading, highlightedQuote, onSelectText }: Props) {
  if (loading) {
    return <main className="reader-pane"><div className="reader-loading">正在装载章节...</div></main>
  }
  if (!paper) {
    return (
      <main className="reader-pane empty-reader">
        <BookOpen size={28} />
        <h2>选择一篇论文开始阅读</h2>
        <p>Outline、符号、引用与 Agent 证据会在同一工作区联动。</p>
      </main>
    )
  }

  const title = section?.title || paper.title
  const content = section?.content || paper.abstractText || '该论文暂时没有可展示的摘要。'
  const symbols = section?.symbols || []
  const references = section?.references || []

  return (
    <main className="reader-pane">
      <header className="reader-header">
        <div className="reader-breadcrumb">{section?.headingPath || '论文概览'}</div>
        <h1>{title}</h1>
        <div className="reader-meta">
          <span>Paper #{paper.id}</span>
          {paper.year && <span>{paper.year}</span>}
          <span>{paper.status || 'READY'}</span>
        </div>
      </header>
      {highlightedQuote && (
        <aside className="evidence-focus">
          <ExternalLink size={15} />
          <span>{highlightedQuote}</span>
        </aside>
      )}
      <article className="markdown-body" onMouseUp={() => {
        const selected = window.getSelection()?.toString().trim()
        if (selected) onSelectText?.(selected.slice(0, 4000))
      }}>
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
      </article>
      {(symbols.length > 0 || references.length > 0) && (
        <footer className="section-context">
          {symbols.length > 0 && (
            <section>
              <h3><Braces size={16} /> 本节符号</h3>
              <div className="context-grid">
                {symbols.map((symbol) => (
                  <div key={symbol.id} className="context-item">
                    <strong>{symbol.latex || symbol.symbol}</strong>
                    <span>{symbol.description || '暂无定义说明'}</span>
                  </div>
                ))}
              </div>
            </section>
          )}
          {references.length > 0 && (
            <section>
              <h3><BookOpen size={16} /> 本节引用</h3>
              <div className="reference-list">
                {references.map((reference) => (
                  <div key={reference.refId} className="reference-row">
                    <span>[{reference.refId}]</span>
                    <strong>{reference.title || reference.rawText || reference.abstractText || '未命名引用'}</strong>
                  </div>
                ))}
              </div>
            </section>
          )}
        </footer>
      )}
    </main>
  )
}
