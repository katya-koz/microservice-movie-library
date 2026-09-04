'use client';

import { useEffect, useRef, useState } from 'react';
import { collectFromDataTransfer, collectFromFileList, DroppedFile } from '@/lib/files';
import { UploadIcon } from './icons';

interface UploadDropzoneProps {
  onFiles: (files: DroppedFile[]) => void;
}

export default function UploadDropzone({ onFiles }: UploadDropzoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [isReading, setIsReading] = useState(false);
  const dragCounter = useRef(0);
  const filesInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    folderInputRef.current?.setAttribute('webkitdirectory', '');
    folderInputRef.current?.setAttribute('directory', '');
  }, []);

  async function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    dragCounter.current = 0;
    setIsDragging(false);
    setIsReading(true);
    try {
      onFiles(await collectFromDataTransfer(e.dataTransfer));
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
      className={`rounded-lg border-2 border-dashed p-10 text-center transition-colors ${
        isDragging ? 'border-marquee bg-marquee/[0.06]' : 'border-ink-line bg-ink-raised/40'
      }`}
    >
      <UploadIcon className="mx-auto h-8 w-8 text-paper-faint" />
      <p className="mt-3 text-paper">{isReading ? 'Reading files…' : 'Drag files or folders here'}</p>
      <p className="mt-1 font-mono text-xs text-paper-muted">or</p>

      <div className="mt-3 flex justify-center gap-3">
        <button
          type="button"
          onClick={() => filesInputRef.current?.click()}
          className="rounded border border-ink-line px-4 py-2 font-mono text-xs uppercase tracking-widest text-paper-muted hover:border-paper-faint hover:text-paper"
        >
          Choose files
        </button>
        <button
          type="button"
          onClick={() => folderInputRef.current?.click()}
          className="rounded border border-ink-line px-4 py-2 font-mono text-xs uppercase tracking-widest text-paper-muted hover:border-paper-faint hover:text-paper"
        >
          Choose a folder
        </button>
      </div>

      <input ref={filesInputRef} type="file" multiple className="hidden" onChange={handleInputChange} />
      <input ref={folderInputRef} type="file" multiple className="hidden" onChange={handleInputChange} />
    </div>
  );
}
