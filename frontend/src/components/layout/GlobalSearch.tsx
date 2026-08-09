import { useEffect, useMemo, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { FolderOpen, History, Search, SearchX, X } from 'lucide-react';

import { FileIcon } from '@/components/files/FileIcon';
import { APP_ROUTES } from '@/constants/routes';
import { useClickOutside } from '@/hooks/useClickOutside';
import { useDebounce } from '@/hooks/useDebounce';
import { MIN_SEARCH_QUERY_LENGTH, useFileSearchQuery } from '@/hooks/useFiles';
import { useFoldersQuery } from '@/hooks/useFolders';
import { useRecentSearches } from '@/hooks/useRecentSearches';
import { useFilesStore } from '@/store/filesStore';
import { useFoldersStore } from '@/store/foldersStore';
import type { FileItem, Folder } from '@/types';
import { cn } from '@/utils/cn';
import { formatBytes } from '@/utils/format';
import { filterFolders } from '@/utils/folder';

const SEARCH_DEBOUNCE_MS = 250;

const IS_MAC =
  typeof navigator !== 'undefined' && navigator.platform.toLowerCase().includes('mac');

type SearchResult =
  | { kind: 'file'; file: FileItem }
  | { kind: 'folder'; folder: Folder };

export interface GlobalSearchProps {
  className?: string;
}

/**
 * Navbar search that looks across the user's cloud: files via the server-side
 * `/files/search` endpoint, folders via the (cached) folder list filtered by
 * name. Selecting a result jumps to the matching explorer pre-filtered.
 *
 * Shortcuts: `⌘/Ctrl + K` focuses the input from anywhere; `Esc` closes the
 * dropdown. Past queries are kept (localStorage) under "Recent searches".
 */
export function GlobalSearch({ className }: GlobalSearchProps) {
  const navigate = useNavigate();
  const [value, setValue] = useState('');
  const [focused, setFocused] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { recents, add: addRecent, remove: removeRecent, clear: clearRecents } =
    useRecentSearches();

  const debounced = useDebounce(value, SEARCH_DEBOUNCE_MS);

  const filesQuery = useFileSearchQuery(debounced);
  const foldersQuery = useFoldersQuery();

  const query = debounced.trim();
  const searching = query.length >= MIN_SEARCH_QUERY_LENGTH;

  const fileResults = useMemo(() => filesQuery.data ?? [], [filesQuery.data]);
  const folderResults = useMemo(
    () => filterFolders(foldersQuery.data ?? [], debounced),
    [foldersQuery.data, debounced],
  );

  const results = useMemo<SearchResult[]>(() => {
    const folders: SearchResult[] = folderResults.map((folder) => ({ kind: 'folder', folder }));
    const files: SearchResult[] = fileResults.map((file) => ({ kind: 'file', file }));
    return [...folders, ...files];
  }, [folderResults, fileResults]);

  // Keyboard selection — navigates the recents list while empty, results while searching.
  const navLength = searching ? results.length : recents.length;
  const safeIndex = Math.min(activeIndex, Math.max(navLength - 1, 0));

  // ⌘/Ctrl + K focuses the search from anywhere (GitHub/Linear-style).
  useEffect(() => {
    const onKeyDown = (event: globalThis.KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        // Only act when the input is actually visible (it's hidden on small screens).
        if (inputRef.current && inputRef.current.offsetParent !== null) {
          event.preventDefault();
          inputRef.current.focus();
          inputRef.current.select();
        }
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  useClickOutside(containerRef, () => setFocused(false));

  const openResult = (result: SearchResult) => {
    setFocused(false);
    setValue('');
    setActiveIndex(0);
    addRecent(debounced);
    if (result.kind === 'file') {
      useFilesStore.getState().setSearchQuery(debounced);
      navigate(APP_ROUTES.files);
    } else {
      useFoldersStore.getState().setSearchQuery(debounced);
      navigate(APP_ROUTES.folders);
    }
  };

  const selectRecent = (recent: string) => {
    setValue(recent);
    setActiveIndex(0);
    inputRef.current?.focus();
  };

  const handleKeyDown = (event: ReactKeyboardEvent<HTMLInputElement>) => {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        if (navLength > 0) {
          setActiveIndex((index) => (index + 1) % navLength);
        }
        break;
      case 'ArrowUp':
        event.preventDefault();
        if (navLength > 0) {
          setActiveIndex((index) => (index - 1 + navLength) % navLength);
        }
        break;
      case 'Enter': {
        if (searching) {
          const result = results[safeIndex];
          if (result) {
            event.preventDefault();
            openResult(result);
          }
        } else {
          const recent = recents[safeIndex];
          if (recent) {
            event.preventDefault();
            selectRecent(recent);
          }
        }
        break;
      }
      case 'Escape':
        setFocused(false);
        inputRef.current?.blur();
        break;
    }
  };

  const showPanel = focused && (value.trim().length > 0 || recents.length > 0);

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <Search className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
      <input
        ref={inputRef}
        type="search"
        value={value}
        onChange={(event) => {
          setValue(event.target.value);
          setActiveIndex(0);
        }}
        onFocus={() => setFocused(true)}
        onKeyDown={handleKeyDown}
        placeholder="Search files and folders…"
        aria-label="Search files and folders"
        aria-expanded={showPanel}
        aria-controls="global-search-results"
        className="focus:border-brand-500 focus:ring-brand-500/25 h-10 w-full rounded-lg border border-gray-300 bg-white pr-16 pl-9 text-sm text-gray-900 shadow-sm transition-colors placeholder:text-gray-400 focus:ring-2 focus:outline-none dark:border-gray-700 dark:bg-gray-950 dark:text-white dark:placeholder:text-gray-500"
      />
      {value ? (
        <button
          type="button"
          onClick={() => setValue('')}
          aria-label="Clear search"
          className="absolute top-1/2 right-2.5 -translate-y-1/2 rounded p-0.5 text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
        >
          <X className="h-4 w-4" />
        </button>
      ) : (
        <kbd className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 rounded-md border border-gray-200 bg-gray-50 px-1.5 py-0.5 font-sans text-[10px] font-medium text-gray-400 dark:border-gray-700 dark:bg-gray-800/60 dark:text-gray-500">
          {IS_MAC ? '⌘K' : 'Ctrl K'}
        </kbd>
      )}

      <AnimatePresence>
        {showPanel && (
          <motion.div
            id="global-search-results"
            role="listbox"
            aria-label="Search results"
            initial={{ opacity: 0, y: -6, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.98 }}
            transition={{ duration: 0.14, ease: 'easeOut' }}
            className="absolute top-full right-0 left-0 z-40 mt-2 overflow-hidden rounded-xl border border-gray-200 bg-white shadow-xl shadow-gray-900/10 dark:border-gray-700 dark:bg-gray-900 dark:shadow-black/40"
          >
            {!searching ? (
              value.trim().length >= MIN_SEARCH_QUERY_LENGTH ? (
                <p className="px-4 py-3.5 text-sm text-gray-400 dark:text-gray-500">
                  Searching…
                </p>
              ) : value.trim() ? (
                <p className="px-4 py-3.5 text-sm text-gray-400 dark:text-gray-500">
                  Type at least {MIN_SEARCH_QUERY_LENGTH} characters to search your cloud…
                </p>
              ) : recents.length > 0 ? (
                <>
                  <div className="flex items-center justify-between border-b border-gray-100 px-4 py-2.5 dark:border-gray-800">
                    <p className="text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400">
                      Recent searches
                    </p>
                    <button
                      type="button"
                      onClick={clearRecents}
                      className="text-xs font-medium text-gray-400 transition-colors hover:text-rose-500 dark:text-gray-500 dark:hover:text-rose-400"
                    >
                      Clear all
                    </button>
                  </div>
                  <ul className="max-h-64 overflow-y-auto py-1.5">
                    {recents.map((recent, index) => {
                      const isActive = index === safeIndex;
                      return (
                        <li
                          key={recent}
                          className={cn(
                            'group flex items-center transition-colors',
                            isActive && 'bg-brand-500/10 dark:bg-brand-400/10',
                          )}
                        >
                          <button
                            type="button"
                            role="option"
                            aria-selected={isActive}
                            onMouseEnter={() => setActiveIndex(index)}
                            onClick={() => selectRecent(recent)}
                            className="flex min-w-0 flex-1 items-center gap-3 px-3 py-2.5 text-left"
                          >
                            <History className="h-4 w-4 shrink-0 text-gray-400 dark:text-gray-500" />
                            <span className="truncate text-sm text-gray-800 dark:text-gray-200">
                              {recent}
                            </span>
                          </button>
                          <button
                            type="button"
                            onClick={() => removeRecent(recent)}
                            aria-label={`Remove “${recent}” from recent searches`}
                            className="mr-2 shrink-0 rounded p-1 text-gray-400 opacity-0 transition-opacity hover:bg-gray-100 hover:text-gray-600 group-hover:opacity-100 focus-visible:opacity-100 dark:hover:bg-gray-800 dark:hover:text-gray-300"
                          >
                            <X className="h-3.5 w-3.5" />
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                </>
              ) : (
                <p className="px-4 py-3.5 text-sm text-gray-400 dark:text-gray-500">
                  Search your files and folders — press {IS_MAC ? '⌘K' : 'Ctrl K'} anytime.
                </p>
              )
            ) : (
              <>
                <div className="flex items-center justify-between border-b border-gray-100 px-4 py-2.5 dark:border-gray-800">
                  <p className="text-xs font-semibold tracking-wide text-gray-500 uppercase dark:text-gray-400">
                    Results for “{query}”
                  </p>
                  <span className="text-xs text-gray-400 dark:text-gray-500">
                    {results.length}
                  </span>
                </div>

                {results.length === 0 ? (
                  <div className="flex flex-col items-center gap-2 px-6 py-8 text-center">
                    <SearchX className="h-6 w-6 text-gray-300 dark:text-gray-600" />
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      {filesQuery.isLoading
                        ? 'Searching…'
                        : `No files or folders match “${query}”.`}
                    </p>
                  </div>
                ) : (
                  <ul className="max-h-80 overflow-y-auto py-1.5">
                    {results.map((result, index) => {
                      const name =
                        result.kind === 'file' ? result.file.originalFileName : result.folder.name;
                      const isActive = index === safeIndex;
                      return (
                        <li
                          key={`${result.kind}-${result.kind === 'file' ? result.file.id : result.folder.id}`}
                        >
                          <button
                            type="button"
                            role="option"
                            aria-selected={isActive}
                            onMouseEnter={() => setActiveIndex(index)}
                            onClick={() => openResult(result)}
                            className={cn(
                              'flex w-full items-center gap-3 px-3 py-2.5 text-left transition-colors',
                              isActive
                                ? 'bg-brand-500/10 dark:bg-brand-400/10'
                                : 'hover:bg-gray-50 dark:hover:bg-gray-800/50',
                            )}
                          >
                            {result.kind === 'file' ? (
                              <FileIcon file={result.file} size="sm" />
                            ) : (
                              <span className="bg-amber-500/10 text-amber-600 dark:text-amber-400 grid h-8 w-8 shrink-0 place-items-center rounded-lg">
                                <FolderOpen className="h-4 w-4" />
                              </span>
                            )}
                            <span className="min-w-0 flex-1">
                              <span className="block truncate text-sm font-medium text-gray-900 dark:text-white">
                                {name}
                              </span>
                              <span className="block text-xs text-gray-400 dark:text-gray-500">
                                {result.kind === 'file'
                                  ? formatBytes(result.file.fileSize)
                                  : 'Folder'}
                              </span>
                            </span>
                            <span className="shrink-0 text-[10px] font-semibold tracking-wider text-gray-400 uppercase dark:text-gray-500">
                              {result.kind}
                            </span>
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </>
            )}

            {filesQuery.isError && (
              <p className="border-t border-gray-100 px-4 py-2 text-xs text-rose-600 dark:border-gray-800 dark:text-rose-400">
                File search is unavailable right now — folders are still shown.
              </p>
            )}

            <div className="border-t border-gray-100 bg-gray-50/60 px-4 py-1.5 text-[11px] text-gray-400 dark:border-gray-800 dark:bg-gray-800/30 dark:text-gray-500">
              ↑↓ navigate · ↵ open · esc close
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
