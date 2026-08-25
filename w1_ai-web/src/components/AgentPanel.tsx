import { ArrowUp, BookOpen, Bot, Eye, Lightbulb, RotateCcw, Search, Square, Wrench } from 'lucide-react'
import { FormEvent, useEffect, useRef, useState } from 'react'
import { streamResearch } from '../api'
import type { AgentEvent, Evidence, PaperSummary } from '../types'
import { AcademicMarkdown } from './AcademicMarkdown'

type Props = {
  sessionId: string
  paperId?: number
  sectionId?: string
  sectionTitle?: string
  headingPath?: string
  selectedText?: string
  scopePapers?: PaperSummary[]
  variant?: 'sidebar' | 'workspace'
  onClearScope?: () => void
  onConversationStarted?: (question: string) => void
  onEvidenceSelect: (evidence: Evidence) => void
}

type Activity = { kind: 'thought' | 'action' | 'observation'; label: string; step?: number }
type ChatTurn = {
  question: string
  answer: string
  activities: Activity[]
  evidence: Evidence[]
  error: string
}

function readStoredTurns(sessionId: string): ChatTurn[] {
  try {
    const stored = window.localStorage.getItem(`scholarbrain.chat.${sessionId}`)
    if (!stored) return []
    const parsed = JSON.parse(stored) as ChatTurn[]
    return Array.isArray(parsed) ? parsed.slice(-30) : []
  } catch {
    return []
  }
}

function storeTurns(sessionId: string, turns: ChatTurn[]) {
  const key = `scholarbrain.chat.${sessionId}`
  const recentTurns = turns.slice(-30)
  try {
    window.localStorage.setItem(key, JSON.stringify(recentTurns))
  } catch {
    const compactTurns = recentTurns.slice(-10).map((turn) => ({
      ...turn,
      activities: turn.activities.map((activity) => ({
        ...activity,
        label: activity.label.slice(0, 1200)
      }))
    }))
    try { window.localStorage.setItem(key, JSON.stringify(compactTurns)) } catch { /* Storage may be unavailable. */ }
  }
}

export function AgentPanel({ sessionId, paperId, sectionId, sectionTitle, headingPath, selectedText, scopePapers = [], variant = 'sidebar', onClearScope, onConversationStarted, onEvidenceSelect }: Props) {
  const [question, setQuestion] = useState('')
  const [submittedQuestion, setSubmittedQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [activities, setActivities] = useState<Activity[]>([])
  const [evidence, setEvidence] = useState<Evidence[]>([])
  const [history, setHistory] = useState<ChatTurn[]>(() => readStoredTurns(sessionId))
  const [running, setRunning] = useState(false)
  const [error, setError] = useState('')
  const cancelRef = useRef<(() => void) | null>(null)

  useEffect(() => {
    const turns = [...history]
    if (submittedQuestion) turns.push({ question: submittedQuestion, answer, activities, evidence, error })
    storeTurns(sessionId, turns)
  }, [sessionId, history, submittedQuestion, answer, activities, evidence, error])

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
    if ((event.type === 'THOUGHT' || event.type === 'MESSAGE') && event.content?.trim()) {
      setActivities((items) => [...items, { kind: 'thought', label: event.content!.trim(), step: event.step }])
      return
    }
    if (event.type === 'ACTION') {
      setActivities((items) => [...items, { kind: 'action', label: event.content || '研究工具', step: event.step }])
      return
    }
    if (event.type === 'OBSERVATION') {
      setActivities((items) => [...items, { kind: 'observation', label: event.content?.trim() || '工具未返回可展示内容', step: event.step }])
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const value = question.trim()
    if (!value || running) return
    if (submittedQuestion) {
      setHistory((turns) => [...turns, { question: submittedQuestion, answer, activities, evidence, error }])
    }
    setSubmittedQuestion(value)
    setQuestion('')
    setAnswer('')
    setEvidence([])
    setActivities([])
    setError('')
    setRunning(true)
    onConversationStarted?.(value)
    const context = variant === 'workspace'
      ? { paperIds: scopePapers.map((item) => item.id) }
      : { paperIds: paperId ? [paperId] : [], sectionId, headingPath, selectedText }
    cancelRef.current = streamResearch(
      sessionId,
      value,
      context,
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
              <span>
                <small>当前研究范围</small>
                <strong>{scopePapers.length ? `已选择 ${scopePapers.length} 篇论文` : '全部论文库'}</strong>
                {scopePapers.length > 0 && <em>{scopePapers.map((item) => item.title).join(' · ')}</em>}
              </span>
            </div>
            {scopePapers.length > 0 && onClearScope && <button type="button" className="secondary-button" onClick={onClearScope}><RotateCcw size={15} />恢复全库</button>}
          </section>
        )}
        {!submittedQuestion && history.length === 0 && (
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
        <div className="chat-thread">
          {[...history, ...(submittedQuestion ? [{ question: submittedQuestion, answer, activities, evidence, error }] : [])].map((turn, turnIndex) => {
            const isActiveTurn = turnIndex === history.length && Boolean(submittedQuestion)
            return (
              <article className="chat-turn" key={`${turn.question}-${turnIndex}`}>
                <div className="chat-message chat-message-user">
                  <div className="chat-bubble chat-bubble-user">{turn.question}</div>
                </div>
                <div className="chat-message chat-message-assistant">
                  <span className="chat-avatar" aria-hidden="true"><Bot size={16} /></span>
                  <div className="chat-bubble chat-bubble-assistant">
                    {(turn.activities.length > 0 || (isActiveTurn && running)) && (
                      <details className="agent-activity" open={isActiveTurn && running}>
                        <summary>{isActiveTurn && running ? '正在研究' : `研究过程 · ${turn.activities.length} 步`}</summary>
                        <div className="activity-list">
                          {turn.activities.map((item, index) => (
                            <div key={`${item.kind}-${index}`} className={`activity-row activity-${item.kind}`}>
                              {item.kind === 'thought' ? <Lightbulb size={14} /> : item.kind === 'action' ? <Wrench size={14} /> : <Eye size={14} />}
                              <div className="activity-content">
                                <strong>{item.kind === 'thought' ? '思考' : item.kind === 'action' ? '调用工具' : '工具返回'}</strong>
                                <div className="activity-message"><AcademicMarkdown>{item.label}</AcademicMarkdown></div>
                              </div>
                            </div>
                          ))}
                          {isActiveTurn && running && <div className="activity-row is-active"><Search size={14} /><span>正在核对论文证据</span></div>}
                        </div>
                      </details>
                    )}
                    {turn.answer ? (
                      <div className="agent-answer"><AcademicMarkdown>{turn.answer}</AcademicMarkdown></div>
                    ) : isActiveTurn && running ? (
                      <div className="assistant-thinking"><span /><span /><span /></div>
                    ) : null}
                    {turn.evidence.length > 0 && (
                      <section className="evidence-list">
                        <div className="evidence-heading"><h3>证据链</h3><span>{turn.evidence.length}</span></div>
                        {turn.evidence.map((item, index) => (
                          <button type="button" key={item.evidenceKey || index} className="evidence-item" onClick={() => onEvidenceSelect(item)}>
                            <span className="evidence-index">{String(index + 1).padStart(2, '0')}</span>
                            <span>
                              <strong><AcademicMarkdown inline>{item.headingPath || item.heading || item.paperTitle || '论文证据'}</AcademicMarkdown></strong>
                              <small><AcademicMarkdown inline>{item.quote || `来自 ${item.accessedVia || 'ResearchAgent'}`}</AcademicMarkdown></small>
                            </span>
                          </button>
                        ))}
                      </section>
                    )}
                    {turn.error && <div className="inline-error">{turn.error}</div>}
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      </div>
      <form className="agent-composer" onSubmit={submit}>
        <textarea value={question} onChange={(event) => setQuestion(event.target.value)} placeholder={variant === 'workspace' ? '输入跨论文或单篇研究问题...' : '这篇论文的核心贡献是什么？'} rows={3} />
        <div className="composer-footer">
          <span>{variant === 'workspace' ? (scopePapers.length ? `限定 ${scopePapers.length} 篇论文` : '搜索全部论文库') : sectionId ? `当前章节 · ${sectionTitle || sectionId}` : paperId ? `当前论文 · Paper #${paperId}` : '未附加论文上下文'}</span>
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
