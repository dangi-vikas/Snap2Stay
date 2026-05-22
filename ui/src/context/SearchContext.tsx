import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import type { VisualSearchResponseWithDebug } from '@/types/api';

interface SearchState {
  uploadedFile: File | null;
  uploadedPreview: string | null;        // object URL for the preview
  result: VisualSearchResponseWithDebug | null;
  isAnalyzing: boolean;
  error: string | null;
  destination: string | null;            // user-entered location filter
}

interface SearchContextValue extends SearchState {
  setUpload: (file: File) => void;
  setResult: (result: VisualSearchResponseWithDebug) => void;
  setAnalyzing: (v: boolean) => void;
  setError: (msg: string | null) => void;
  setDestination: (destination: string | null) => void;
  reset: () => void;
}

const SearchContext = createContext<SearchContextValue | null>(null);

const initialState: SearchState = {
  uploadedFile: null,
  uploadedPreview: null,
  result: null,
  isAnalyzing: false,
  error: null,
  destination: null,
};

export function SearchProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SearchState>(initialState);

  const setUpload = useCallback((file: File) => {
    setState((prev) => {
      // Revoke any existing preview URL to avoid leaks
      if (prev.uploadedPreview) {
        URL.revokeObjectURL(prev.uploadedPreview);
      }
      return {
        ...prev,
        uploadedFile: file,
        uploadedPreview: URL.createObjectURL(file),
        result: null,
        error: null,
      };
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

  const setDestination = useCallback((destination: string | null) => {
    setState((prev) => ({ ...prev, destination }));
  }, []);

  const reset = useCallback(() => {
    setState((prev) => {
      if (prev.uploadedPreview) URL.revokeObjectURL(prev.uploadedPreview);
      return initialState;
    });
  }, []);

  const value = useMemo<SearchContextValue>(
    () => ({ ...state, setUpload, setResult, setAnalyzing, setError, setDestination, reset }),
    [state, setUpload, setResult, setAnalyzing, setError, setDestination, reset],
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
