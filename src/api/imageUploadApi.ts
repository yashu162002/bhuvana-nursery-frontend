import http from './http';

interface ImageUploadResponse {
  url: string;
  filename: string;
}

export async function uploadPlantImage(
  file: File,
): Promise<ImageUploadResponse> {
  const body = new FormData();
  body.append('file', file);

  const response = await http.post<ImageUploadResponse>(
    '/admin/uploads',
    body,
  );

  return response.data;
}
