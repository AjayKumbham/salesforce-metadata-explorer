import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const connectionId = authService.currentConnectionId();

  if (connectionId) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${connectionId}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403 || error.status === 500) {
        if (error.error?.message?.includes('Invalid or expired')) {
          authService.logout();
        } else if (error.status === 401 || error.status === 403) {
           authService.logout();
        }
      }
      return throwError(() => error);
    })
  );
};
