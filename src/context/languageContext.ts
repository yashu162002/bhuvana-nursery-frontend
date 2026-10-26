import { createContext, useContext } from 'react'

export type Language = 'kn' | 'en'

export interface LanguageContextValue {
  language: Language
  isKannada: boolean
  toggle: () => void
}

export const LanguageContext = createContext<LanguageContextValue>({
  language: 'kn',
  isKannada: true,
  toggle: () => {},
})

export function useLanguage(): LanguageContextValue {
  return useContext(LanguageContext)
}
