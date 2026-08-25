import { ChevronDown, ChevronRight, FileText } from 'lucide-react'
import { useState } from 'react'
import type { OutlineNode } from '../types'
import { AcademicMarkdown } from './AcademicMarkdown'

type Props = {
  nodes: OutlineNode[]
  selectedId?: string
  onSelect: (node: OutlineNode) => void
}

function Branch({ node, selectedId, onSelect }: { node: OutlineNode } & Omit<Props, 'nodes'>) {
  const [expanded, setExpanded] = useState(true)
  const hasChildren = node.children?.length > 0

  return (
    <li>
      <div className={`outline-row ${selectedId === node.id ? 'is-selected' : ''}`}>
        <button
          type="button"
          className="tree-toggle"
          aria-label={expanded ? '收起子章节' : '展开子章节'}
          onClick={() => setExpanded((value) => !value)}
          disabled={!hasChildren}
        >
          {hasChildren ? (expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />) : <FileText size={13} />}
        </button>
        <button type="button" className="outline-title" onClick={() => onSelect(node)} title={node.title}>
          <AcademicMarkdown inline>{node.title}</AcademicMarkdown>
        </button>
      </div>
      {hasChildren && expanded && (
        <ul className="outline-children">
          {node.children.map((child) => (
            <Branch key={child.id} node={child} selectedId={selectedId} onSelect={onSelect} />
          ))}
        </ul>
      )}
    </li>
  )
}

export function OutlineTree({ nodes, selectedId, onSelect }: Props) {
  if (!nodes.length) {
    return <div className="compact-empty">该论文尚未生成 Outline</div>
  }
  return (
    <ul className="outline-tree">
      {nodes.map((node) => <Branch key={node.id} node={node} selectedId={selectedId} onSelect={onSelect} />)}
    </ul>
  )
}
