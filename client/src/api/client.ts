import type { ApiEnvelope, AuthenticationResponse, Session } from './types';

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

/**
 * Transport for the Spring API. Deliberately free of React and of platform APIs (no
 * localStorage, no SecureStore, no `window`) so the same file serves Android, iOS and web --
 * persistence is delegated to whoever sets `onSessionChange`.
 */
export class ApiClient {
  private session: Session | null = null;
  private refreshing: Promise<boolean> | null = null;

  /** Called whenever the session changes, including when a failed refresh clears it. */
  onSessionChange: (session: Session | null) => void = () => {};

  constructor(private readonly baseUrl: string) {}

  getSession(): Session | null {
    return this.session;
  }

  /**
   * @param persist false when rehydrating from storage at boot -- the value came *from* storage,
   *                so echoing it back would be a redundant write.
   */
  setSession(session: Session | null, { persist = true }: { persist?: boolean } = {}): void {
    this.session = session;
    if (persist) this.onSessionChange(session);
  }

  get<T>(path: string) {
    return this.request<T>('GET', path);
  }
  post<T>(path: string, body?: unknown) {
    return this.request<T>('POST', path, body);
  }
  put<T>(path: string, body?: unknown) {
    return this.request<T>('PUT', path, body);
  }
  delete<T>(path: string) {
    return this.request<T>('DELETE', path);
  }

  /** Unauthenticated request -- for /auth/login and /auth/signup, which reject a stale bearer. */
  postPublic<T>(path: string, body?: unknown) {
    return this.request<T>('POST', path, body, { auth: false });
  }

  private async request<T>(
    method: Method,
    path: string,
    body?: unknown,
    { auth = true }: { auth?: boolean } = {},
  ): Promise<T> {
    let res = await this.send(method, path, body, auth);

    // Access tokens live ~6 minutes (question.expires.in), so this path is hit constantly.
    if (res.status === 401 && auth && this.session) {
      if (await this.refreshOnce()) {
        res = await this.send(method, path, body, auth);
      }
    }

    return this.unwrap<T>(res);
  }

  private send(method: Method, path: string, body: unknown, auth: boolean): Promise<Response> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth && this.session) headers.Authorization = `Bearer ${this.session.accessToken}`;

    return fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }

  /**
   * Collapses concurrent refreshes into one request. Without this, a screen firing three
   * queries at once would spend three refresh tokens and race to store the last one.
   */
  private refreshOnce(): Promise<boolean> {
    this.refreshing ??= this.doRefresh().finally(() => {
      this.refreshing = null;
    });
    return this.refreshing;
  }

  private async doRefresh(): Promise<boolean> {
    const current = this.session;
    if (!current) return false;

    let res: Response;
    try {
      res = await this.send(
        'POST',
        '/auth/refresh',
        { userId: current.user.id, refreshToken: current.refreshToken },
        false,
      );
    } catch {
      // Network failure, not a rejected token -- keep the session so a retry can succeed.
      return false;
    }

    let accessToken: string | undefined;
    if (res.ok) {
      const body = (await res.json().catch(() => null)) as ApiEnvelope<AuthenticationResponse> | null;
      accessToken = body?.data?.accessToken ?? undefined;
    }

    if (!accessToken) {
      // The refresh token is spent, expired, or was overwritten by a login on another device
      // (the server stores one refresh token per user, not per device).
      this.setSession(null);
      return false;
    }

    // /auth/refresh returns ONLY accessToken -- refreshToken and user come back null, so
    // spreading the current session is what keeps them alive.
    this.setSession({ ...current, accessToken });
    return true;
  }

  private async unwrap<T>(res: Response): Promise<T> {
    // Void handlers (@ResponseStatus(NO_CONTENT)) send no body: DELETEs, change-password.
    if (res.status === 204) return undefined as T;

    const text = await res.text();
    let body: Record<string, unknown> | null = null;
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        // Not our envelope. JWTAuthenticationEntryPoint calls response.sendError(), which
        // renders Spring Boot's default error page rather than Response<T>.
      }
    }

    if (!res.ok || body?.success === false) {
      const message =
        (typeof body?.message === 'string' && body.message) ||
        (typeof body?.error === 'string' && body.error) ||
        `Request failed with status ${res.status}`;
      throw new ApiError(message, res.status);
    }

    return (body?.data ?? undefined) as T;
  }
}
