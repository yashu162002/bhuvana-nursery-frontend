import http from './http';

export interface AdminCategory {
  id: string;
  slug: string;
  nameKn: string;
  nameEn: string;
  descriptionKn: string | null;
  descriptionEn: string | null;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryFormData {
  nameKn: string;
  nameEn: string;
  slug: string;
  descriptionKn: string;
  descriptionEn: string;
  published: boolean;
}

export async function getAdminCategories(): Promise<AdminCategory[]> {
  const response = await http.get<AdminCategory[]>('/admin/categories');
  return response.data;
}

export async function createCategory(
  form: CategoryFormData,
): Promise<AdminCategory> {
  const response = await http.post('/admin/categories', {
    ...form,
    slug: form.slug.trim() || null,
    descriptionKn: form.descriptionKn.trim() || null,
    descriptionEn: form.descriptionEn.trim() || null,
  });

  return response.data;
}

export async function updateCategory(
  id: string,
  form: CategoryFormData,
): Promise<AdminCategory> {
  const response = await http.put(`/admin/categories/${id}`, {
    ...form,
    slug: form.slug.trim() || null,
    descriptionKn: form.descriptionKn.trim() || null,
    descriptionEn: form.descriptionEn.trim() || null,
  });

  return response.data;
}

export async function setCategoryPublished(
  id: string,
  value: boolean,
): Promise<AdminCategory> {
  const response = await http.patch(
    `/admin/categories/${id}/publish`,
    null,
    { params: { value } },
  );

  return response.data;
}
