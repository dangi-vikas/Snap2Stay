import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import type { VisualSearchResponseWithDebug } from '@/types/api';
import heic2any from 'heic2any';

interface SearchState {
  uploadedFile: File | null;
  uploadedPreview: string | null;        // object URL for the preview
  result: VisualSearchResponseWithDebug | null;
  isAnalyzing: boolean;
  error: string | null;
}

interface SearchContextValue extends SearchState {
  setUpload: (file: File) => void;
  setResult: (result: VisualSearchResponseWithDebug) => void;
  setAnalyzing: (v: boolean) => void;
  setError: (msg: string | null) => void;
  reset: () => void;
}

const SearchContext = createContext<SearchContextValue | null>(null);

const initialState: SearchState = {
  uploadedFile: null,
  uploadedPreview: null,
  result: null,
  isAnalyzing: false,
  error: null,
};

function isHeicFile(file: File): boolean {
  return (
    file.type === 'image/heic' ||
    file.type === 'image/heif' ||
    /\.(heic|heif)$/i.test(file.name)
  );
}

async function createPreviewUrl(file: File): Promise<string> {
  if (isHeicFile(file)) {
    const blob = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.8 });
    const result = Array.isArray(blob) ? blob[0] : blob;
    return URL.createObjectURL(result);
  }
  return URL.createObjectURL(file);
}

export function SearchProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SearchState>(initialState);

  const setUpload = useCallback((file: File) => {
    setState((prev) => {
      if (prev.uploadedPreview) {
        URL.revokeObjectURL(prev.uploadedPreview);
      }
      return {
        ...prev,
        uploadedFile: file,
        uploadedPreview: null,
        result: null,
        error: null,
      };
    });
    createPreviewUrl(file).then((url) => {
      setState((prev) => ({ ...prev, uploadedPreview: url }));
    });
  }, []);

  const setResult = useCallback((result: VisualSearchResponseWithDebug) => {
    setState((prev) => ({ ...prev, result, isAnalyzing: false, error: null }));
  }, []);

  const setAnalyzing = useCallback((v: boolean) => {
    setState((prev) => ({ ...prev, isAnalyzing: v }));
  }, []);

  const setError = useCallback((msg: string | null) => {
    setState((prev) => ({ ...prev, error: msg, isAnalyzing: false }));
  }, []);

  const reset = useCallback(() => {
    setState((prev) => {
      if (prev.uploadedPreview) URL.revokeObjectURL(prev.uploadedPreview);
      return initialState;
    });
  }, []);

  const value = useMemo<SearchContextValue>(
    () => ({ ...state, setUpload, setResult, setAnalyzing, setError, reset }),
    [state, setUpload, setResult, setAnalyzing, setError, reset],
  );

  return <SearchContext.Provider value={value}>{children}</SearchContext.Provider>;
}

export function useSearch(): SearchContextValue {
  const ctx = useContext(SearchContext);
  if (!ctx) {
    throw new Error('useSearch must be used inside <SearchProvider>');
  }
  return ctx;
}
