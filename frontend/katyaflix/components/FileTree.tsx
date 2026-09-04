'use client';

import { ClapperIcon, FileIcon, FolderIcon, SubtitleIcon } from './icons';
import { TreeFolderNode, TreeNode } from '@/lib/uploadTree';

interface FileTreeProps {
  node: TreeFolderNode;
  canonicalNameById?: Map<string, string>;
  seasonLabelByFolderId?: Map<string, string>;
  ignoredIds?: Set<string>;
  depth?: number;
}

export default function FileTree({ node, canonicalNameById, seasonLabelByFolderId, ignoredIds, depth = 0 }: FileTreeProps) {
  return (
    <ul className={depth === 0 ? 'space-y-0.5' : 'ml-3 space-y-0.5 border-l border-ink-line pl-3'}>
      {node.children.map((child) => (
        <TreeRow
          key={child.id}
          node={child}
          canonicalNameById={canonicalNameById}
          seasonLabelByFolderId={seasonLabelByFolderId}
          ignoredIds={ignoredIds}
          depth={depth}
        />
      ))}
    </ul>
  );
}

function TreeRow({
  node,
  canonicalNameById,
  seasonLabelByFolderId,
  ignoredIds,
  depth,
}: {
  node: TreeNode;
  canonicalNameById?: Map<string, string>;
  seasonLabelByFolderId?: Map<string, string>;
  ignoredIds?: Set<string>;
  depth: number;
}) {
  if (node.kind === 'folder') {
    const seasonLabel = depth === 0 ? seasonLabelByFolderId?.get(node.id) : undefined;
    return (
      <li>
        {/* Folders collapsed by default — no `open` attribute */}
        <details>
          <summary className="flex cursor-pointer list-none items-center gap-2 rounded px-1.5 py-1 text-sm text-paper marker:content-none hover:bg-ink-elevated">
            <FolderIcon className="h-4 w-4 flex-shrink-0 text-paper-faint" />
            <span className="truncate">{node.name}</span>
            {seasonLabel && <span className="ticket-tag flex-shrink-0">{seasonLabel}</span>}
            <span className="ml-auto flex-shrink-0 font-mono text-[10px] text-paper-faint">{node.children.length}</span>
          </summary>
          <FileTree
            node={node}
            canonicalNameById={canonicalNameById}
            seasonLabelByFolderId={seasonLabelByFolderId}
            ignoredIds={ignoredIds}
            depth={depth + 1}
          />
        </details>
      </li>
    );
  }

  const canonicalName = canonicalNameById?.get(node.id);
  const isIgnored = node.role === 'other' || ignoredIds?.has(node.id);

  return (
    <li className={`flex items-center gap-2 rounded px-1.5 py-1 text-sm ${isIgnored ? 'opacity-40' : ''}`}>
      {node.role === 'video' && <ClapperIcon className="h-4 w-4 flex-shrink-0 text-paper-faint" />}
      {node.role === 'subtitle' && <SubtitleIcon className="h-4 w-4 flex-shrink-0 text-paper-faint" />}
      {node.role === 'other' && <FileIcon className="h-4 w-4 flex-shrink-0 text-paper-faint" />}
      <span className="truncate text-paper-muted">{node.name}</span>
      {canonicalName && canonicalName !== node.name && (
        <span className="flex-shrink-0 font-mono text-[11px] text-marquee">→ {canonicalName}</span>
      )}
      {isIgnored && <span className="ml-auto flex-shrink-0 font-mono text-[10px] text-paper-faint">ignored</span>}
    </li>
  );
}
