import http from './http';

export interface AdminUser {
  username: string;
  role: 'ADMIN';
}

export async function prepareCsrf(): Promise<void> {
  await http.get('/auth/csrf');
}

export async function login(
  username: string,
  password: string,
): Promise<AdminUser> {
  await prepareCsrf();

  const response = await http.post<AdminUser>('/auth/login', {
    username,
    password,
  });

  return response.data;
}

export async function getCurrentAdmin(): Promise<AdminUser | null> {
  try {
    const response = await http.get<AdminUser>('/auth/me');
    return response.data;
  } catch {
    return null;
  }
}

export async function logout(): Promise<void> {
  await prepareCsrf();
  await http.post('/auth/logout');
}
