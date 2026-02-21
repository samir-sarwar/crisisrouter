/**
 * API utility – wraps fetch with credentials: 'include' by default.
 *
 * All paths are relative (e.g. '/api/users/me'), handled by:
 *   - Vite dev proxy in development
 *   - Vercel rewrites in production
 *
 * This keeps all traffic on the same domain so session cookies work.
 */
export function api(path: string, init?: RequestInit): Promise<Response> {
    return fetch(path, {
        credentials: 'include',
        ...init,
    });
}
