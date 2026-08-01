import { AlertTriangle, ArrowRight, CheckCircle2, GitBranch, MinusCircle } from 'lucide-react'
import type { PaperRelation } from '../types'

type Props = {
  relations: PaperRelation[]
  onOpenPaper: (paperId: number) => void
}

function RelationRow({ relation, onOpenPaper }: { relation: PaperRelation; onOpenPaper: Props['onOpenPaper'] }) {
  const pending = relation.auditStatus === 'PENDING_REVIEW'
  const unrelated = relation.type === 'UNRELATED'
  return (
    <button type="button" className="relation-row" onClick={() => onOpenPaper(relation.relatedId)}>
      <span className={`relation-icon ${pending ? 'pending' : unrelated ? 'muted' : 'confirmed'}`}>
        {pending ? <AlertTriangle size={16} /> : unrelated ? <MinusCircle size={16} /> : <CheckCircle2 size={16} />}
      </span>
      <span className="relation-content">
        <span className="relation-label"><b>{relation.type}</b><small>{Math.round((relation.confidence || 0) * 100)}%</small></span>
        <strong>{relation.relatedTitle}</strong>
        <small>{relation.description || relation.auditStatus || '暂无关系说明'}</small>
      </span>
      <ArrowRight size={15} />
    </button>
  )
}

export function RelationsPanel({ relations, onOpenPaper }: Props) {
  const confirmed = relations.filter((item) => item.auditStatus !== 'PENDING_REVIEW' && item.type !== 'UNRELATED')
  const pending = relations.filter((item) => item.auditStatus === 'PENDING_REVIEW')
  const history = relations.filter((item) => item.auditStatus !== 'PENDING_REVIEW' && item.type === 'UNRELATED')

  return (
    <div className="relations-view">
      <header className="view-header">
        <div><span className="eyebrow">KNOWLEDGE GRAPH</span><h2>论文关系审计</h2></div>
        <span className="count-badge">{relations.length}</span>
      </header>
      {!relations.length && <div className="large-empty"><GitBranch size={24} /><strong>尚未生成论文关系</strong><span>LibrarianAgent 审计完成后会在这里展示证据与置信度。</span></div>}
      {confirmed.length > 0 && <section className="relation-group"><h3>已确认关系</h3>{confirmed.map((item) => <RelationRow key={`${item.relatedId}-${item.type}`} relation={item} onOpenPaper={onOpenPaper} />)}</section>}
      {pending.length > 0 && <section className="relation-group"><h3>待复核</h3>{pending.map((item) => <RelationRow key={`${item.relatedId}-${item.type}`} relation={item} onOpenPaper={onOpenPaper} />)}</section>}
      {history.length > 0 && <section className="relation-group"><h3>审计历史</h3>{history.map((item) => <RelationRow key={`${item.relatedId}-${item.type}`} relation={item} onOpenPaper={onOpenPaper} />)}</section>}
    </div>
  )
}
