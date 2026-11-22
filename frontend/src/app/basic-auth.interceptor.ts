import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const basicAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const credentials = inject(AuthService).credentials();
  if (!credentials || !request.url.startsWith('/api/')) return next(request);

  const bytes = new TextEncoder().encode(`${credentials.username}:${credentials.password}`);
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join('');
  return next(request.clone({ setHeaders: { Authorization: `Basic ${btoa(binary)}` } }));
};
