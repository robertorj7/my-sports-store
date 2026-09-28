import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

export function errorMessage(error: unknown, fallback = 'Ocorreu um erro inesperado.'): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Não foi possível conectar à API.';
    const body = error.error as Partial<ApiError> | null;
    if (body?.fieldErrors) return Object.values(body.fieldErrors).join(' ');
    if (body?.message) return body.message;
  }
  return fallback;
}
