import http from './http';

export interface AdminHeroSlide {
  id: string;
  imageUrl: string;
  altText: string | null;
  titleKn: string | null;
  titleEn: string | null;
  displayOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface HeroSlideFormData {
  imageUrl: string;
  altText?: string;
  titleKn?: string;
  titleEn?: string;
  displayOrder: number;
  active: boolean;
}

/**
 * Public endpoint to fetch active hero slides in display order.
 */
export async function getHeroSlides(): Promise<AdminHeroSlide[]> {
  const response = await http.get<AdminHeroSlide[]>('/hero-slides');
  return response.data;
}

/**
 * Admin endpoint to fetch all hero slides including inactive ones.
 */
export async function getAdminHeroSlides(): Promise<AdminHeroSlide[]> {
  const response = await http.get<AdminHeroSlide[]>('/admin/hero-slides');
  return response.data;
}

/**
 * Admin endpoint to create a new hero slide.
 */
export async function createAdminHeroSlide(
  data: HeroSlideFormData,
): Promise<AdminHeroSlide> {
  const response = await http.post<AdminHeroSlide>('/admin/hero-slides', data);
  return response.data;
}

/**
 * Admin endpoint to update an existing hero slide.
 */
export async function updateAdminHeroSlide(
  id: string,
  data: HeroSlideFormData,
): Promise<AdminHeroSlide> {
  const response = await http.put<AdminHeroSlide>(
    `/admin/hero-slides/${id}`,
    data,
  );
  return response.data;
}

/**
 * Admin endpoint to toggle a hero slide's active status.
 */
export async function toggleAdminHeroSlideActive(
  id: string,
): Promise<AdminHeroSlide> {
  const response = await http.patch<AdminHeroSlide>(
    `/admin/hero-slides/${id}/toggle-active`,
  );
  return response.data;
}

/**
 * Admin endpoint to delete a hero slide.
 */
export async function deleteAdminHeroSlide(id: string): Promise<void> {
  await http.delete(`/admin/hero-slides/${id}`);
}

/**
 * Admin endpoint to reorder hero slides.
 */
export async function reorderAdminHeroSlides(
  orderedIds: string[],
): Promise<void> {
  await http.post('/admin/hero-slides/reorder', orderedIds);
}

/**
 * Admin endpoint to reset all hero slides back to the 6 default slides.
 */
export async function resetDefaultHeroSlides(): Promise<AdminHeroSlide[]> {
  const response = await http.post<AdminHeroSlide[]>(
    '/admin/hero-slides/reset-defaults',
  );
  return response.data;
}
