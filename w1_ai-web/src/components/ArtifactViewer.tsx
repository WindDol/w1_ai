import { Check, Copy, FileCode2, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { api } from '../api'
import type { ArtifactContent, ArtifactSummary, ArtifactType } from '../types'

type Props = {
  jobId: string
  artifacts: ArtifactSummary[]
  initialType?: ArtifactType
  onClose: () => void
}

export function ArtifactViewer({ jobId, artifacts, initialType, onClose }: Props) {
  const available = artifacts.filter((item) => item.available)
  const [type, setType] = useState<ArtifactType>(initialType || available[0]?.type || 'RAW_MARKDOWN')
  const [artifact, setArtifact] = useState<ArtifactContent>()
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    api.getArtifact(jobId, type)
      .then(setArtifact)
      .catch((cause: Error) => setError(cause.message))
      .finally(() => setLoading(false))
  }, [jobId, type])

  async function copy() {
    if (!artifact) return
    await navigator.clipboard.writeText(artifact.content)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1500)
  }

  const markdown = type === 'RAW_MARKDOWN' || type === 'NORMALIZED_MARKDOWN'

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="artifact-modal" role="dialog" aria-modal="true" aria-label="解析产物审阅" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-header">
          <div><span className="eyebrow">INGEST ARTIFACT</span><h2>解析产物审阅</h2></div>
          <div className="header-actions">
            <button type="button" className="icon-button" onClick={copy} title="复制内容">{copied ? <Check size={17} /> : <Copy size={17} />}</button>
            <button type="button" className="icon-button" onClick={onClose} title="关闭"><X size={18} /></button>
          </div>
        </header>
        <div className="artifact-tabs" role="tablist">
          {available.map((item) => (
            <button type="button" role="tab" aria-selected={item.type === type} className={item.type === type ? 'is-active' : ''} key={item.type} onClick={() => setType(item.type)}>
              {item.label}
            </button>
          ))}
        </div>
        <div className="artifact-content">
          {loading && <div className="compact-empty">正在读取产物...</div>}
          {error && <div className="inline-error">{error}</div>}
          {!loading && artifact && markdown && <div className="markdown-body artifact-markdown"><ReactMarkdown remarkPlugins={[remarkGfm]}>{artifact.content}</ReactMarkdown></div>}
          {!loading && artifact && !markdown && <pre><FileCode2 size={16} />{artifact.content}</pre>}
        </div>
        {artifact?.truncated && <div className="truncated-notice">内容过大，当前只展示前 2,000,000 个字符。</div>}
      </section>
    </div>
  )
}
