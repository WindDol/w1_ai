import { ArrowUp, BookOpen, Bot, CheckCircle2, FileSearch, Search, Square, Wrench } from 'lucide-react'
import { FormEvent, useRef, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { streamResearch } from '../api'
import type { AgentEvent, Evidence } from '../types'

type Props = {
  paperId?: number
  paperTitle?: string
  sectionId?: string
  sectionTitle?: string
  headingPath?: string
  selectedText?: string
  variant?: 'sidebar' | 'workspace'
  onOpenReader?: () => void
  onEvidenceSelect: (evidence: Evidence) => void
}

type Activity = { kind: 'action' | 'observation'; label: string; step?: number }

export function AgentPanel({ paperId, paperTitle, sectionId, sectionTitle, headingPath, selectedText, variant = 'sidebar', onOpenReader, onEvidenceSelect }: Props) {
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [activities, setActivities] = useState<Activity[]>([])
  const [evidence, setEvidence] = useState<Evidence[]>([])
  const [running, setRunning] = useState(false)
  const [error, setError] = useState('')
  const cancelRef = useRef<(() => void) | null>(null)

  function handleEvent(event: AgentEvent) {
    if (event.type === 'ANSWER') {
      setAnswer(event.content || '')
      return
    }
    if (event.type === 'EVIDENCE' && typeof event.data === 'string') {
      try {
        setEvidence(JSON.parse(event.data) as Evidence[])
      } catch {
        setError('证据数据解析失败')
      }
      return
    }
    if (event.type === 'ACTION') {
      setActivities((items) => [...items, { kind: 'action', label: `调用 ${event.content || '研究工具'}`, step: event.step }])
      return
    }
    if (event.type === 'OBSERVATION') {
      setActivities((items) => [...items, { kind: 'observation', label: '已读取并整理工具结果', step: event.step }])
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const value = question.trim()
    if (!value || running) return
    setAnswer('')
    setEvidence([])
    setActivities([])
    setError('')
    setRunning(true)
    cancelRef.current = streamResearch(
      value,
      { paperId, sectionId, headingPath, selectedText },
      handleEvent,
      () => setRunning(false),
      (message) => { setError(message); setRunning(false) }
    )
  }

  function stop() {
    cancelRef.current?.()
    setRunning(false)
  }

  return (
    <aside className={`agent-panel ${variant === 'workspace' ? 'agent-workspace' : ''}`}>
      <div className="panel-heading agent-heading">
        <div>
          <span className="eyebrow">{variant === 'workspace' ? 'RESEARCH MISSION' : 'RESEARCH AGENT'}</span>
          <h2>{variant === 'workspace' ? '深度研究工作台' : '研究助手'}</h2>
        </div>
        <span className={`agent-state ${running ? 'is-running' : ''}`}>{running ? '检索中' : '就绪'}</span>
      </div>
      <div className="agent-conversation">
        {variant === 'workspace' && (
          <section className="research-context-bar">
            <div>
              <span className="context-icon"><BookOpen size={16} /></span>
              <span><small>当前研究范围{selectedText ? ` · 已选择 ${selectedText.length} 字原文` : ''}</small><strong>{paperTitle || '全部论文库'}</strong>{sectionTitle && <em>{sectionTitle}</em>}</span>
            </div>
            {paperId && onOpenReader && <button type="button" className="secondary-button" onClick={onOpenReader}><FileSearch size={15} />核查原文</button>}
          </section>
        )}
        {!answer && !activities.length && (
          <div className="agent-empty">
            <Bot size={25} />
            <strong>{variant === 'workspace' ? '提出一个研究问题' : '围绕当前论文提问'}</strong>
            <span>{variant === 'workspace' ? 'ResearchAgent 会规划检索、阅读章节、追踪符号与引用，并给出可回查证据。' : '回答会附带可回查的章节、chunk 与原文证据。'}</span>
            {variant === 'workspace' && (
              <div className="prompt-suggestions">
                <button type="button" onClick={() => setQuestion('这篇论文解决了什么问题，核心方法和结论分别是什么？')}>梳理核心贡献</button>
                <button type="button" onClick={() => setQuestion('解释论文中的关键数学符号及其在推导中的作用。')}>解释关键符号</button>
                <button type="button" onClick={() => setQuestion('基于库内论文比较该工作的后续发展、支持证据和局限。')}>跨论文研究</button>
              </div>
            )}
          </div>
        )}
        {activities.length > 0 && (
          <section className="agent-activity">
            <h3>执行记录</h3>
            {activities.map((item, index) => (
              <div key={`${item.kind}-${index}`} className="activity-row">
                {item.kind === 'action' ? <Wrench size={14} /> : <CheckCircle2 size={14} />}
                <span>{item.label}</span>
              </div>
            ))}
            {running && <div className="activity-row is-active"><Search size={14} /><span>正在核对论文证据</span></div>}
          </section>
        )}
        {answer && (
          <section className="agent-answer">
            <h3>回答</h3>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{answer}</ReactMarkdown>
          </section>
        )}
        {evidence.length > 0 && (
          <section className="evidence-list">
            <div className="evidence-heading"><h3>证据链</h3><span>{evidence.length}</span></div>
            {evidence.map((item, index) => (
              <button type="button" key={item.evidenceKey || index} className="evidence-item" onClick={() => onEvidenceSelect(item)}>
                <span className="evidence-index">{String(index + 1).padStart(2, '0')}</span>
                <span>
                  <strong>{item.headingPath || item.heading || item.paperTitle || '论文证据'}</strong>
                  <small>{item.quote || `来自 ${item.accessedVia || 'ResearchAgent'}`}</small>
                </span>
              </button>
            ))}
          </section>
        )}
        {error && <div className="inline-error">{error}</div>}
      </div>
      <form className="agent-composer" onSubmit={submit}>
        <textarea value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="这篇论文的核心贡献是什么？" rows={3} />
        <div className="composer-footer">
          <span>{sectionId ? `当前章节 · ${sectionTitle || sectionId}` : paperId ? `当前论文 · Paper #${paperId}` : '全部论文库'}</span>
          {running ? (
            <button type="button" className="icon-button dark" onClick={stop} title="停止"><Square size={15} /></button>
          ) : (
            <button type="submit" className="icon-button dark" disabled={!question.trim()} title="发送"><ArrowUp size={17} /></button>
          )}
        </div>
      </form>
    </aside>
  )
}
