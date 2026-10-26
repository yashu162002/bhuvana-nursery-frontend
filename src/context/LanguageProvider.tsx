import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { LanguageContext, type Language } from './languageContext'

const STORAGE_KEY = 'bhoovana-language'

function readStoredLanguage(): Language {
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY) || window.localStorage.getItem('bhuvana-language')
    return stored === 'en' ? 'en' : 'kn'
  } catch {
    return 'kn'
  }
}

export default function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>(readStoredLanguage)

  useEffect(() => {
    document.documentElement.lang = language
    try {
      window.localStorage.setItem(STORAGE_KEY, language)
    } catch {
      /* storage unavailable - language just won't persist */
    }
  }, [language])

  const toggle = useCallback(
    () => setLanguage((current) => (current === 'kn' ? 'en' : 'kn')),
    [],
  )

  const value = useMemo(
    () => ({ language, isKannada: language === 'kn', toggle }),
    [language, toggle],
  )

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}
