export type ApiEnvelope<T> = { code: string; info: string; data: T }

export type PaperSummary = {
  id: number
  title: string
  status?: string
  createdAt?: string
}

export type OutlineNode = {
  id: string
  title: string
  level: number
  children: OutlineNode[]
}

export type SymbolItem = {
  id: number
  paperId: number
  symbol: string
  latex?: string
  description?: string
  definitionFormula?: string
}

export type ReferenceItem = {
  id?: number
  refId: string
  rawText?: string
  title?: string
  abstractText?: string
  linkedPaperId?: number
  citationCount?: number
}

export type PaperRelation = {
  relatedId: number
  relatedTitle: string
  type: string
  description?: string
  direction?: string
  confidence?: number
  auditStatus?: string
  supportingEvidence?: string
  conflictingEvidence?: string
  auditVersion?: string
  modelName?: string
  retrievalVersion?: string
}

export type PaperWorkspace = {
  id: number
  title: string
  abstractText?: string
  status?: string
  year?: number
  createdAt?: string
  latestJobId?: string
  outline: OutlineNode[]
  symbols: SymbolItem[]
  references: ReferenceItem[]
  relations: PaperRelation[]
}

export type SectionWorkspace = {
  id: string
  paperId: number
  paperTitle: string
  title: string
  parentId?: string
  index?: number
  headingPath: string
  content: string
  symbols: SymbolItem[]
  references: ReferenceItem[]
}

export type IngestJob = {
  jobId: string
  paperId?: number
  originalFilename: string
  fileSha256?: string
  fileSize?: number
  status: string
  currentStage?: string
  failedStage?: string
  errorCode?: string
  errorMessage?: string
  attemptCount?: number
  parserType?: string
  parserVersion?: string
  normalizerVersion?: string
  createdAt?: string
  updatedAt?: string
  startedAt?: string
  completedAt?: string
}

export type StageRun = {
  id: number
  stage: string
  attempt: number
  status: string
  errorCode?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
}

export type ArtifactSummary = {
  type: ArtifactType
  label: string
  available: boolean
}

export type ArtifactType =
  | 'RAW_MARKDOWN'
  | 'NORMALIZED_MARKDOWN'
  | 'NORMALIZATION_REPORT'
  | 'METADATA'

export type IngestionWorkspace = {
  job: IngestJob
  stageRuns: StageRun[]
  artifacts: ArtifactSummary[]
}

export type ArtifactContent = {
  type: ArtifactType
  label: string
  content: string
  truncated: boolean
}

export type Evidence = {
  evidenceKey?: string
  evidenceType?: string
  paperId?: number
  paperTitle?: string
  sectionId?: string
  heading?: string
  chunkId?: string
  headingPath?: string
  quote?: string
  accessedVia?: string
  pageStart?: number
  pageEnd?: number
  referenceIndex?: string
}

export type AgentEvent = {
  type?: string
  content?: string
  data?: string | Record<string, unknown>
  step?: number
}
