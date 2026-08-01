import type {
  AgentEvent,
  ApiEnvelope,
  ArtifactContent,
  ArtifactType,
  IngestJob,
  IngestionWorkspace,
  PaperSummary,
  PaperWorkspace,
  SectionWorkspace
} from './types'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  let payload: ApiEnvelope<T>
  try {
    payload = await response.json() as ApiEnvelope<T>
  } catch {
    throw new Error(`服务返回了无法解析的响应（HTTP ${response.status}）`)
  }
  if (!response.ok || payload.code !== '0000') {
    throw new Error(payload.info || `请求失败（HTTP ${response.status}）`)
  }
  return payload.data
}

export const api = {
  listPapers: () => request<PaperSummary[]>('/api/v1/paper/list'),
  getPaper: (paperId: number) => request<PaperWorkspace>(`/api/v1/paper/${paperId}/reading`),
  getSection: (sectionId: string) => request<SectionWorkspace>(`/api/v1/paper/sections/${sectionId}`),
  getIngestion: (jobId: string) => request<IngestionWorkspace>(`/api/v1/paper/ingestions/${jobId}/details`),
  getArtifact: (jobId: string, type: ArtifactType) =>
    request<ArtifactContent>(`/api/v1/paper/ingestions/${jobId}/artifacts/${type}`),
  upload: (file: File) => {
    const body = new FormData()
    body.append('file', file)
    return request<IngestJob>('/api/v1/paper/upload', { method: 'POST', body })
  },
  retry: (jobId: string) => request<IngestJob>(`/api/v1/paper/ingestions/${jobId}/retry`, { method: 'POST' }),
  rerun: (jobId: string, stage: string) =>
    request<IngestJob>(`/api/v1/paper/ingestions/${jobId}/rerun?stage=${encodeURIComponent(stage)}`, { method: 'POST' })
}

export function streamResearch(
  question: string,
  context: { paperId?: number; sectionId?: string; headingPath?: string; selectedText?: string },
  onEvent: (event: AgentEvent) => void,
  onDone: () => void,
  onError: (message: string) => void
) {
  const sessionId = crypto.randomUUID()
  const query = new URLSearchParams({ sessionId, question })
  if (context.paperId) query.set('paperId', String(context.paperId))
  if (context.sectionId) query.set('sectionId', context.sectionId)
  if (context.headingPath) query.set('headingPath', context.headingPath)
  if (context.selectedText) query.set('selectedText', context.selectedText.slice(0, 4000))
  const source = new EventSource(`/api/v1/agent/ask-stream?${query.toString()}`)
  let completed = false
  source.onmessage = (message) => {
    try {
      onEvent(JSON.parse(message.data) as AgentEvent)
    } catch {
      onEvent({ type: 'MESSAGE', content: message.data })
    }
  }
  source.addEventListener('done', () => {
    completed = true
    source.close()
    onDone()
  })
  source.addEventListener('agent-error', (event) => {
    completed = true
    source.close()
    onError((event as MessageEvent<string>).data || 'Agent 执行失败')
  })
  source.onerror = () => {
    source.close()
    if (!completed) {
      onError('Agent 连接意外中断')
    }
  }
  return () => source.close()
}
