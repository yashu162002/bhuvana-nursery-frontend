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

export interface MessageResponse {
  message: string;
}

/**
 * Asks the backend to email a single-use reset link. The response message is
 * identical whether or not the address belongs to an administrator, so this
 * never reveals whether an account exists.
 */
export async function requestPasswordReset(email: string): Promise<string> {
  await prepareCsrf();

  const response = await http.post<MessageResponse>(
    '/auth/password/forgot',
    { email },
  );

  return response.data.message;
}

/** Redeems the emailed token and persists the new password on the server. */
export async function resetPassword(
  token: string,
  password: string,
): Promise<string> {
  await prepareCsrf();

  const response = await http.post<MessageResponse>(
    '/auth/password/reset',
    { token, password },
  );

  return response.data.message;
}

/** Extracts the server's generic message, falling back to a friendly default. */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const response = (
      err as { response?: { status?: number; data?: { message?: string } } }
    ).response;

    const message = response?.data?.message;
    if (typeof message === 'string' && message.trim().length > 0) {
      return message.trim();
    }

    const status = response?.status;
    if (status === 403) {
      return 'Security token expired. Please refresh and try again.';
    }
    if (status === 429) {
      return 'Too many requests. Please wait a few minutes and try again.';
    }
    if (status === 503) {
      return 'Password reset is temporarily unavailable. Please try again later.';
    }
  }

  return fallback;
}
