/**
 * API utility – prefixes all requests with the backend URL.
 *
 * In development, VITE_BACKEND_URL is empty (Vite proxy handles /api -> localhost:8080).
 * In production, VITE_BACKEND_URL = https://crisisrouter-production.up.railway.app
 * so requests go directly to Railway (with credentials / cookies).
 */
const BACKEND = import.meta.env.VITE_BACKEND_URL || '';

export function api(path: string, init?: RequestInit): Promise<Response> {
    return fetch(`${BACKEND}${path}`, {
        credentials: 'include',
        ...init,
    });
}
