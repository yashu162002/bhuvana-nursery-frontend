import http from './http'
import type {
  Plant,
  PlantCategory,
  PlantPage,
  PlantFilters,
} from '../features/plants/plantTypes'

export async function getPlants(
  filters: PlantFilters = {},
): Promise<PlantPage> {
  const response = await http.get<PlantPage>('/plants', {
    params: {
      page: filters.page ?? 0,
      size: filters.size ?? 12,
      ...(filters.category ? { category: filters.category } : {}),
      ...(filters.search ? { search: filters.search } : {}),
    },
  })

  return response.data
}

export async function getPlantBySlug(
  slug: string,
): Promise<Plant> {
  const response = await http.get<Plant>(
    `/plants/${encodeURIComponent(slug)}`,
  )

  return response.data
}

export async function getCategories(): Promise<PlantCategory[]> {
  const response = await http.get<PlantCategory[]>('/categories')
  return response.data
}
