import { AlertCircle, CheckCircle2, Clock3, FileCode2, LoaderCircle, RefreshCw, RotateCcw, UploadCloud, X } from 'lucide-react'
import { ChangeEvent, DragEvent, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import type { ArtifactType, IngestionWorkspace, StageRun } from '../types'
import { ArtifactViewer } from './ArtifactViewer'

const STAGES = [
  'FILE_VALIDATION', 'DEDUPLICATION', 'OCR_PARSING', 'STRUCTURE_NORMALIZATION', 'METADATA_EXTRACTION',
  'PAPER_PERSISTENCE', 'SYMBOL_EXTRACTION', 'REFERENCE_EXTRACTION', 'EMBEDDING', 'RELATION_AUDIT'
]

const STAGE_LABELS: Record<string, string> = {
  FILE_VALIDATION: '文件校验', DEDUPLICATION: '重复检测', OCR_PARSING: 'OCR 解析',
  STRUCTURE_NORMALIZATION: '结构归一化', METADATA_EXTRACTION: '元数据提取', PAPER_PERSISTENCE: '论文持久化',
  SYMBOL_EXTRACTION: '符号提取', REFERENCE_EXTRACTION: '引用提取', EMBEDDING: '向量化', RELATION_AUDIT: '关系审计'
}

type Props = {
  open: boolean
  initialJobId?: string
  onClose: () => void
  onReady: (paperId: number) => void
}

function latestRun(stageRuns: StageRun[], stage: string) {
  return stageRuns.filter((run) => run.stage === stage).sort((a, b) => b.attempt - a.attempt)[0]
}

export function IngestionDrawer({ open, initialJobId, onClose, onReady }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [workspace, setWorkspace] = useState<IngestionWorkspace>()
  const [jobId, setJobId] = useState(initialJobId)
  const [busy, setBusy] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState('')
  const [artifactType, setArtifactType] = useState<ArtifactType>()
  const [rerunStage, setRerunStage] = useState('STRUCTURE_NORMALIZATION')

  useEffect(() => setJobId(initialJobId), [initialJobId])

  useEffect(() => {
    if (!open || !jobId) return
    let cancelled = false
    let timer: number | undefined
    const poll = async () => {
      try {
        const value = await api.getIngestion(jobId)
        if (cancelled) return
        setWorkspace(value)
        setError('')
        if (!['READY', 'FAILED'].includes(value.job.status)) timer = window.setTimeout(poll, 1500)
      } catch (cause) {
        if (!cancelled) setError((cause as Error).message)
      }
    }
    void poll()
    return () => { cancelled = true; if (timer) window.clearTimeout(timer) }
  }, [open, jobId])

  async function upload(file?: File) {
    if (!file || file.type !== 'application/pdf') {
      setError('请选择 PDF 文件')
      return
    }
    setBusy(true)
    setError('')
    try {
      const job = await api.upload(file)
      setJobId(job.jobId)
      setWorkspace(undefined)
    } catch (cause) {
      setError((cause as Error).message)
    } finally {
      setBusy(false)
    }
  }

  async function retry() {
    if (!jobId) return
    setBusy(true)
    try { await api.retry(jobId); setWorkspace(await api.getIngestion(jobId)) }
    catch (cause) { setError((cause as Error).message) }
    finally { setBusy(false) }
  }

  async function rerun() {
    if (!jobId) return
    setBusy(true)
    try { await api.rerun(jobId, rerunStage); setWorkspace(await api.getIngestion(jobId)) }
    catch (cause) { setError((cause as Error).message) }
    finally { setBusy(false) }
  }

  function drop(event: DragEvent) {
    event.preventDefault()
    setDragging(false)
    void upload(event.dataTransfer.files[0])
  }

  if (!open) return null
  const job = workspace?.job
  const ready = job?.status === 'READY' && job.paperId

  return (
    <div className="drawer-backdrop" role="presentation" onMouseDown={onClose}>
      <aside className="ingestion-drawer" role="dialog" aria-modal="true" aria-label="论文摄取任务" onMouseDown={(event) => event.stopPropagation()}>
        <header className="modal-header">
          <div><span className="eyebrow">INGESTION PIPELINE</span><h2>论文摄取</h2></div>
          <button type="button" className="icon-button" onClick={onClose} title="关闭"><X size={18} /></button>
        </header>
        {!jobId && (
          <div
            className={`upload-dropzone ${dragging ? 'is-dragging' : ''}`}
            onDragOver={(event) => { event.preventDefault(); setDragging(true) }}
            onDragLeave={() => setDragging(false)}
            onDrop={drop}
          >
            {busy ? <LoaderCircle className="spin" size={28} /> : <UploadCloud size={30} />}
            <strong>上传科研 PDF</strong>
            <span>文件提交后可以关闭窗口，后台任务会继续执行。</span>
            <button type="button" className="primary-button" onClick={() => inputRef.current?.click()} disabled={busy}>选择文件</button>
            <input ref={inputRef} type="file" accept="application/pdf" hidden onChange={(event: ChangeEvent<HTMLInputElement>) => void upload(event.target.files?.[0])} />
          </div>
        )}
        {job && (
          <div className="job-body">
            <section className="job-summary">
              <div><span className={`status-dot status-${job.status.toLowerCase()}`} /> <strong>{job.status}</strong></div>
              <h3>{job.originalFilename}</h3>
              <code>{job.jobId}</code>
              {job.errorMessage && <div className="failure-box"><AlertCircle size={17} /><span><strong>{job.failedStage}</strong>{job.errorMessage}</span></div>}
              <div className="job-actions">
                {job.status === 'FAILED' && <button type="button" className="primary-button" onClick={retry} disabled={busy}><RefreshCw size={15} /> 重试失败阶段</button>}
                {ready && <button type="button" className="primary-button" onClick={() => onReady(job.paperId!)}>打开论文</button>}
                <button type="button" className="secondary-button" onClick={() => { setJobId(undefined); setWorkspace(undefined) }}><UploadCloud size={15} /> 新任务</button>
              </div>
            </section>
            <section className="timeline-section">
              <div className="section-title"><h3>阶段时间线</h3><span>Attempt {job.attemptCount || 1}</span></div>
              <ol className="stage-timeline">
                {STAGES.map((stage) => {
                  const run = latestRun(workspace.stageRuns, stage)
                  const status = run?.status || (job.currentStage === stage ? 'RUNNING' : 'PENDING')
                  return (
                    <li key={stage} className={`stage-${status.toLowerCase()}`}>
                      <span className="stage-marker">{status === 'COMPLETED' ? <CheckCircle2 size={16} /> : status === 'FAILED' ? <AlertCircle size={16} /> : status === 'RUNNING' ? <LoaderCircle className="spin" size={16} /> : <Clock3 size={15} />}</span>
                      <span><strong>{STAGE_LABELS[stage]}</strong><small>{run?.errorMessage || status}</small></span>
                      {run?.finishedAt && <time>{new Date(run.finishedAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}</time>}
                    </li>
                  )
                })}
              </ol>
            </section>
            <section className="artifact-section">
              <div className="section-title"><h3>解析产物</h3><span>{workspace.artifacts.filter((item) => item.available).length}/4</span></div>
              <div className="artifact-buttons">
                {workspace.artifacts.map((item) => <button type="button" key={item.type} disabled={!item.available} onClick={() => setArtifactType(item.type)}><FileCode2 size={15} />{item.label}</button>)}
              </div>
            </section>
            {job.paperId && (
              <section className="rerun-section">
                <div className="section-title"><h3>指定阶段重跑</h3></div>
                <div className="rerun-controls">
                  <select value={rerunStage} onChange={(event) => setRerunStage(event.target.value)}>{STAGES.slice(2).map((stage) => <option value={stage} key={stage}>{STAGE_LABELS[stage]}</option>)}</select>
                  <button type="button" className="secondary-button" onClick={rerun} disabled={busy}><RotateCcw size={15} /> 重跑</button>
                </div>
              </section>
            )}
          </div>
        )}
        {error && <div className="inline-error drawer-error">{error}</div>}
      </aside>
      {workspace && artifactType && <ArtifactViewer jobId={workspace.job.jobId} artifacts={workspace.artifacts} initialType={artifactType} onClose={() => setArtifactType(undefined)} />}
    </div>
  )
}
