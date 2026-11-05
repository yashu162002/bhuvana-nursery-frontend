/**
 * useLocalBusinessSchema – injects the persistent LocalBusiness JSON-LD on
 * every public page. Must be called once from PublicLayout (or App root).
 */
import { useEffect } from 'react'
import { LOCAL_BUSINESS_SCHEMA, injectJsonLd } from './schema'

export function useLocalBusinessSchema() {
  useEffect(() => {
    return injectJsonLd('schema-local-business', LOCAL_BUSINESS_SCHEMA)
  }, [])
}

/**
 * usePlantSchema – injects a Product JSON-LD for a specific plant page.
 */
import { buildPlantSchema } from './schema'
import type { Plant } from '../features/plants/plantTypes'

export function usePlantSchema(plant: Plant | null) {
  useEffect(() => {
    if (!plant) return
    return injectJsonLd('schema-plant-product', buildPlantSchema(plant))
  }, [plant])
}
