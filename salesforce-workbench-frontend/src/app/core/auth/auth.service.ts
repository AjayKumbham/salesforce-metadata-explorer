import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of, map } from 'rxjs';
import { Router } from '@angular/router';

export interface AuthResponse {
  connectionId: string;
  instanceUrl: string;
  username: string;
  userId: string;
  organizationId: string;
  apiVersion?: string;
  environment?: string;
  loginStatus: string;
}

export interface UserSession {
  connectionId: string;
  username: string;
  instanceUrl: string;
  organizationId: string;
  userId: string;
  apiVersion?: string;
  environment?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private sessionState = signal<UserSession | null>(this.loadSessionFromStorage());

  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly currentUser = computed(() => this.sessionState());
  readonly currentConnectionId = computed(() => this.sessionState()?.connectionId);

  constructor() { }

  private loadSessionFromStorage(): UserSession | null {
    const data = localStorage.getItem('salesforce_session');
    if (data) {
      try {
        return JSON.parse(data);
      } catch (e) {
        return null;
      }
    }
    return null;
  }

  login(loginRequest: any): Observable<boolean> {
    return this.http.post<AuthResponse>('/api/auth/login', loginRequest).pipe(
      tap(response => {
        if (response.connectionId) {
          this.setSession(response);
        }
      }),
      map(res => !!res.connectionId),
      catchError(() => of(false))
    );
  }

  setSessionFromOAuth(connectionId: string): Observable<boolean> {
    return this.http.get<AuthResponse>('/api/auth/session', {
      headers: { Authorization: `Bearer ${connectionId}` }
    }).pipe(
      tap(response => this.setSession(response)),
      map(res => !!res.connectionId),
      catchError(() => of(false))
    );
  }

  private setSession(response: AuthResponse) {
    const session: UserSession = {
      connectionId: response.connectionId,
      username: response.username,
      instanceUrl: response.instanceUrl,
      organizationId: response.organizationId,
      userId: response.userId,
      apiVersion: response.apiVersion,
      environment: response.environment
    };
    
    this.sessionState.set(session);
    
    localStorage.setItem('salesforce_session', JSON.stringify(session));
  }

  logout() {
    const clearSession = () => {
      this.sessionState.set(null);
      localStorage.removeItem('salesforce_session');
      this.router.navigate(['/login']);
    };

    // Call the backend to invalidate the session and remove the DB row
    this.http.post('/api/auth/logout', {}).subscribe({
      next: clearSession,
      error: clearSession // Ensure we clear local session even if the backend call fails
    });
  }
}
