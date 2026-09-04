'use client';

import { useEffect, useRef, useState } from 'react';
import { collectFromDataTransfer, collectFromFileList, DroppedFile } from '@/lib/files';

interface FileDropzoneProps {
  onFiles: (files: DroppedFile[]) => void;
  accept?: string;
  multiple?: boolean;
  /** Enables folder selection (native directory picker + recursive drop reading). */
  directory?: boolean;
  label: string;
  hint?: string;
  compact?: boolean;
}

export default function FileDropzone({
  onFiles,
  accept,
  multiple,
  directory,
  label,
  hint,
  compact,
}: FileDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isReading, setIsReading] = useState(false);
  const dragCounter = useRef(0);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!inputRef.current) return;
    if (directory) {
      inputRef.current.setAttribute('webkitdirectory', '');
      inputRef.current.setAttribute('directory', '');
    } else {
      inputRef.current.removeAttribute('webkitdirectory');
      inputRef.current.removeAttribute('directory');
    }
  }, [directory]);

  async function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    dragCounter.current = 0;
    setIsDragging(false);
    setIsReading(true);
    try {
      const files = await collectFromDataTransfer(e.dataTransfer);
      onFiles(files);
    } finally {
      setIsReading(false);
    }
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (e.target.files) onFiles(collectFromFileList(e.target.files));
    e.target.value = '';
  }

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => inputRef.current?.click()}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          inputRef.current?.click();
        }
      }}
      onDragOver={(e) => e.preventDefault()}
      onDragEnter={(e) => {
        e.preventDefault();
        dragCounter.current += 1;
        setIsDragging(true);
      }}
      onDragLeave={(e) => {
        e.preventDefault();
        dragCounter.current -= 1;
        if (dragCounter.current <= 0) setIsDragging(false);
      }}
      onDrop={handleDrop}
      className={`group cursor-pointer rounded-lg border-2 border-dashed text-center transition-colors ${
        compact ? 'p-5' : 'p-10'
      } ${
        isDragging
          ? 'border-marquee bg-marquee/[0.06]'
          : 'border-ink-line bg-ink-raised/40 hover:border-paper-faint hover:bg-ink-raised'
      }`}
    >
      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept={accept}
        multiple={multiple}
        onChange={handleInputChange}
      />
      <ReelIcon className={`mx-auto ${compact ? 'h-6 w-6' : 'h-9 w-9'} text-paper-faint group-hover:text-marquee`} />
      <p className={`mt-3 font-medium text-paper ${compact ? 'text-sm' : 'text-base'}`}>
        {isReading ? 'Reading files…' : label}
      </p>
      {hint && <p className="mt-1 font-mono text-xs text-paper-muted">{hint}</p>}
    </div>
  );
}

function ReelIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" className={className} aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="12" cy="12" r="1.6" fill="currentColor" />
      <circle cx="12" cy="6.2" r="1.6" fill="currentColor" />
      <circle cx="17.2" cy="9.4" r="1.6" fill="currentColor" />
      <circle cx="15.2" cy="15.6" r="1.6" fill="currentColor" />
      <circle cx="8.8" cy="15.6" r="1.6" fill="currentColor" />
      <circle cx="6.8" cy="9.4" r="1.6" fill="currentColor" />
    </svg>
  );
}
