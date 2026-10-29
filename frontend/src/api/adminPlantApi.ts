import http from './http';
import type { Plant, PlantCategory } from '../features/plants/plantTypes';

export interface AdminPlant extends Plant {
  published: boolean;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PlantFormData {
  categoryId: string;
  nameKn: string;
  nameEn: string;
  scientificName: string;
  descriptionKn: string;
  descriptionEn: string;
  slug: string;
  price: string;
  enquiryOnly: boolean;
  availability: 'AVAILABLE' | 'OUT_OF_STOCK' | 'UNKNOWN';
  published: boolean;
  featured: boolean;
  imageUrl: string;
}

function toPayload(form: PlantFormData) {
  const priceText = form.price.trim();

  return {
    ...form,
    price: priceText === '' ? null : Number(priceText),
    scientificName: form.scientificName.trim() || null,
    descriptionKn: form.descriptionKn.trim() || null,
    descriptionEn: form.descriptionEn.trim() || null,
    slug: form.slug.trim() || null,
    imageUrl: form.imageUrl.trim() || null,
  };
}

export async function getAdminPlants(): Promise<AdminPlant[]> {
  const response = await http.get<AdminPlant[]>('/admin/plants');
  return response.data;
}

export async function createAdminPlant(
  form: PlantFormData,
): Promise<AdminPlant> {
  const response = await http.post<AdminPlant>(
    '/admin/plants',
    toPayload(form),
  );
  return response.data;
}

export async function updateAdminPlant(
  id: string,
  form: PlantFormData,
): Promise<AdminPlant> {
  const response = await http.put<AdminPlant>(
    `/admin/plants/${id}`,
    toPayload(form),
  );
  return response.data;
}

export async function setPlantPublished(
  id: string,
  value: boolean,
): Promise<AdminPlant> {
  const response = await http.patch<AdminPlant>(
    `/admin/plants/${id}/publish`,
    null,
    { params: { value } },
  );
  return response.data;
}

export async function archiveAdminPlant(id: string): Promise<AdminPlant> {
  const response = await http.delete<AdminPlant>(`/admin/plants/${id}`);
  return response.data;
}

export async function getPlantCategories(): Promise<PlantCategory[]> {
  const response = await http.get<PlantCategory[]>('/categories');
  return response.data;
}
