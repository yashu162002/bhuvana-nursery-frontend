export type AvailabilityStatus =
  | 'AVAILABLE'
  | 'OUT_OF_STOCK'
  | 'UNKNOWN';

export interface Plant {
  id: string;
  slug: string;
  nameKn: string;
  nameEn: string;
  scientificName?: string | null;
  descriptionKn?: string | null;
  descriptionEn?: string | null;
  categoryId: string;
  categorySlug?: string;
  categoryNameKn?: string;
  categoryNameEn?: string;
  price: number | null;
  enquiryOnly: boolean;
  availability: AvailabilityStatus;
  featured: boolean;
  imageUrl: string | null;
  published?: boolean;
}

export interface PlantCategory {
  id: string;
  slug: string;
  nameKn: string;
  nameEn: string;
  descriptionKn?: string | null;
  descriptionEn?: string | null;
}

export interface PlantPage {
  content: Plant[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PlantFilters {
  category?: string;
  search?: string;
  page?: number;
  size?: number;
}
