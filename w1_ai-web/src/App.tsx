import { BookOpen, BrainCircuit, Braces, FileSearch, GitBranch, Menu, PanelLeftClose, PanelRightClose, RefreshCw, Upload, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { AgentPanel } from './components/AgentPanel'
import { IngestionDrawer } from './components/IngestionDrawer'
import { OutlineTree } from './components/OutlineTree'
import { PaperLibrary } from './components/PaperLibrary'
import { Reader } from './components/Reader'
import { RelationsPanel } from './components/RelationsPanel'
import type { Evidence, OutlineNode, PaperSummary, PaperWorkspace, SectionWorkspace } from './types'

type CenterView = 'reader' | 'symbols' | 'references' | 'relations'
type ProductMode = 'research' | 'reading'

export default function App() {
  const [papers, setPapers] = useState<PaperSummary[]>([])
  const [paper, setPaper] = useState<PaperWorkspace>()
  const [section, setSection] = useState<SectionWorkspace>()
  const [selectedSectionId, setSelectedSectionId] = useState<string>()
  const [view, setView] = useState<CenterView>('reader')
  const [productMode, setProductMode] = useState<ProductMode>('research')
  const [loadingPapers, setLoadingPapers] = useState(true)
  const [loadingPaper, setLoadingPaper] = useState(false)
  const [loadingSection, setLoadingSection] = useState(false)
  const [error, setError] = useState('')
  const [leftOpen, setLeftOpen] = useState(true)
  const [rightOpen, setRightOpen] = useState(true)
  const [mobileNav, setMobileNav] = useState(false)
  const [ingestionOpen, setIngestionOpen] = useState(false)
  const [highlightedQuote, setHighlightedQuote] = useState<string>()
  const [selectedText, setSelectedText] = useState('')

  const loadPapers = useCallback(async () => {
    setLoadingPapers(true)
    try {
      const values = await api.listPapers()
      setPapers(values)
      setError('')
      if (!paper && values.length) void openPaper(values[0].id)
    } catch (cause) {
      setError((cause as Error).message)
    } finally {
      setLoadingPapers(false)
    }
  // paper is intentionally omitted to keep initial loading stable.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => { void loadPapers() }, [loadPapers])

  async function openPaper(paperId: number) {
    setLoadingPaper(true)
    setSection(undefined)
    setSelectedSectionId(undefined)
    setHighlightedQuote(undefined)
    setSelectedText('')
    setMobileNav(false)
    try {
      setPaper(await api.getPaper(paperId))
      setView('reader')
      setError('')
    } catch (cause) {
      setError((cause as Error).message)
    } finally {
      setLoadingPaper(false)
    }
  }

  async function openSection(nodeOrId: OutlineNode | string, quote?: string) {
    const sectionId = typeof nodeOrId === 'string' ? nodeOrId : nodeOrId.id
    setSelectedSectionId(sectionId)
    setLoadingSection(true)
    setView('reader')
    setHighlightedQuote(quote)
    try {
      setSection(await api.getSection(sectionId))
      setError('')
    } catch (cause) {
      setError((cause as Error).message)
    } finally {
      setLoadingSection(false)
    }
  }

  async function openEvidence(evidence: Evidence) {
    if (evidence.paperId && evidence.paperId !== paper?.id) await openPaper(evidence.paperId)
    if (evidence.sectionId) await openSection(evidence.sectionId, evidence.quote)
    setProductMode('reading')
  }

  const shellClass = `app-shell ${productMode === 'research' ? 'research-mode' : ''} ${leftOpen ? '' : 'left-collapsed'} ${rightOpen ? '' : 'right-collapsed'}`
  const selectedPaperId = paper?.id
  const counts = useMemo(() => ({ symbols: paper?.symbols.length || 0, references: paper?.references.length || 0, relations: paper?.relations.length || 0 }), [paper])

  return (
    <div className={shellClass}>
      <header className="topbar">
        <button type="button" className="mobile-menu icon-button" onClick={() => setMobileNav(true)} title="打开论文库"><Menu size={19} /></button>
        <div className="brand"><span className="brand-mark">SB</span><span><strong>ScholarBrain</strong><small>Research Workspace</small></span></div>
        <div className="mode-switch" role="tablist" aria-label="工作台模式">
          <button type="button" role="tab" aria-selected={productMode === 'research'} className={productMode === 'research' ? 'is-active' : ''} onClick={() => setProductMode('research')}><BrainCircuit size={15} />研究</button>
          <button type="button" role="tab" aria-selected={productMode === 'reading'} className={productMode === 'reading' ? 'is-active' : ''} onClick={() => setProductMode('reading')}><BookOpen size={15} />阅读</button>
        </div>
        <div className="topbar-context"><span>{paper?.title || '科研论文智能阅读系统'}</span></div>
        <div className="topbar-actions">
          <button type="button" className="icon-button desktop-only" onClick={() => setLeftOpen((value) => !value)} title={leftOpen ? '收起论文库' : '展开论文库'}><PanelLeftClose size={18} /></button>
          {productMode === 'reading' && <button type="button" className="icon-button desktop-only" onClick={() => setRightOpen((value) => !value)} title={rightOpen ? '收起 Agent' : '展开 Agent'}><PanelRightClose size={18} /></button>}
          <button type="button" className="upload-button" aria-label="上传论文" onClick={() => setIngestionOpen(true)}><Upload size={16} /><span>上传论文</span></button>
        </div>
      </header>

      <div className={`mobile-library ${mobileNav ? 'is-open' : ''}`}>
        <div className="mobile-library-header"><strong>论文库</strong><button type="button" className="icon-button" onClick={() => setMobileNav(false)}><X size={18} /></button></div>
        <PaperLibrary papers={papers} selectedId={selectedPaperId} loading={loadingPapers} onSelect={(id) => void openPaper(id)} />
      </div>

      <PaperLibrary papers={papers} selectedId={selectedPaperId} loading={loadingPapers} onSelect={(id) => void openPaper(id)} />

      <section className="workspace-center">
        <nav className="workspace-tabs">
          <button type="button" className={view === 'reader' ? 'is-active' : ''} onClick={() => setView('reader')}><BookOpen size={15} />阅读</button>
          <button type="button" className={view === 'symbols' ? 'is-active' : ''} onClick={() => setView('symbols')}><Braces size={15} />符号 <span>{counts.symbols}</span></button>
          <button type="button" className={view === 'references' ? 'is-active' : ''} onClick={() => setView('references')}><FileSearch size={15} />引用 <span>{counts.references}</span></button>
          <button type="button" className={view === 'relations' ? 'is-active' : ''} onClick={() => setView('relations')}><GitBranch size={15} />关系 <span>{counts.relations}</span></button>
          {paper?.latestJobId && <button type="button" className="job-link" onClick={() => setIngestionOpen(true)}>查看摄取任务</button>}
        </nav>
        <div className="workspace-content">
          {view === 'reader' && (
            <aside className="outline-panel">
              <div className="outline-heading"><span className="eyebrow">OUTLINE</span><strong>论文结构</strong></div>
              <OutlineTree nodes={paper?.outline || []} selectedId={selectedSectionId} onSelect={(node) => void openSection(node)} />
            </aside>
          )}
          {view === 'reader' && <Reader paper={paper} section={section} loading={loadingPaper || loadingSection} highlightedQuote={highlightedQuote} onSelectText={setSelectedText} />}
          {view === 'symbols' && <CatalogView title="符号表" empty="该论文尚未提取符号" items={(paper?.symbols || []).map((item) => ({ key: String(item.id), lead: item.latex || item.symbol, title: item.symbol, body: item.description || item.definitionFormula || '暂无定义说明' }))} />}
          {view === 'references' && <CatalogView title="参考文献" empty="该论文尚未提取引用" items={(paper?.references || []).map((item) => ({ key: item.refId, lead: `[${item.refId}]`, title: item.title || '未命名引用', body: item.abstractText || item.rawText || '暂无摘要' }))} />}
          {view === 'relations' && <RelationsPanel relations={paper?.relations || []} onOpenPaper={(id) => void openPaper(id)} />}
        </div>
      </section>

      <AgentPanel
        variant={productMode === 'research' ? 'workspace' : 'sidebar'}
        paperId={paper?.id}
        paperTitle={paper?.title}
        sectionId={section?.id}
        sectionTitle={section?.title}
        headingPath={section?.headingPath}
        selectedText={selectedText}
        onOpenReader={() => setProductMode('reading')}
        onEvidenceSelect={(item) => void openEvidence(item)}
      />

      {error && <div className="global-error"><span>{error}</span><button type="button" onClick={() => void loadPapers()}><RefreshCw size={14} />重试</button></div>}
      <IngestionDrawer open={ingestionOpen} initialJobId={paper?.latestJobId} onClose={() => setIngestionOpen(false)} onReady={(id) => { setIngestionOpen(false); void loadPapers(); void openPaper(id) }} />
    </div>
  )
}

function CatalogView({ title, empty, items }: { title: string; empty: string; items: { key: string; lead: string; title: string; body: string }[] }) {
  return (
    <div className="catalog-view">
      <header className="view-header"><div><span className="eyebrow">PAPER CONTEXT</span><h2>{title}</h2></div><span className="count-badge">{items.length}</span></header>
      {!items.length && <div className="large-empty"><FileSearch size={24} /><strong>{empty}</strong></div>}
      <div className="catalog-list">
        {items.map((item) => <div className="catalog-row" key={item.key}><span className="catalog-lead">{item.lead}</span><span><strong>{item.title}</strong><small>{item.body}</small></span></div>)}
      </div>
    </div>
  )
}
