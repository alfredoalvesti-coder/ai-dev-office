import { Injectable, signal } from '@angular/core';

export interface Credentials {
  username: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly credentials = signal<Credentials | null>(null);
  readonly authenticated = signal(false);

  signIn(username: string, password: string): void {
    this.credentials.set({ username: username.trim(), password });
    this.authenticated.set(false);
  }

  confirmAuthentication(): void {
    this.authenticated.set(true);
  }

  signOut(): void {
    this.credentials.set(null);
    this.authenticated.set(false);
  }
}
