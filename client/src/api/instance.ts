import { API_BASE_URL } from '@/config';

import { ApiClient } from './client';

/**
 * The single client instance, in its own module so the endpoint modules can import it without
 * going through `index.ts` -- that would be a require cycle (index -> posts -> index).
 *
 * Wiring lives here rather than in client.ts so the client itself stays platform-agnostic;
 * `AuthProvider` attaches persistence by assigning `api.onSessionChange`.
 */
export const api = new ApiClient(API_BASE_URL);
