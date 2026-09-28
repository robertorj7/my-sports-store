import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Cart, Order, Product } from './models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  findAll(filter: { category?: string; search?: string } = {}): Observable<Product[]> {
    let params = new HttpParams();
    if (filter.category) params = params.set('category', filter.category);
    if (filter.search) params = params.set('search', filter.search);
    return this.http.get<Product[]>('/api/products', { params });
  }

  categories(): Observable<string[]> {
    return this.http.get<string[]>('/api/products/categories');
  }
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly state = signal<Cart>({ items: [], total: 0 });

  readonly cart = this.state.asReadonly();

  load(): Observable<Cart> {
    return this.http.get<Cart>('/api/cart').pipe(tap((c) => this.state.set(c)));
  }

  setItem(productId: string, quantity: number): Observable<Cart> {
    return this.http.post<Cart>('/api/cart/items', { productId, quantity }).pipe(tap((c) => this.state.set(c)));
  }

  removeItem(productId: string): Observable<Cart> {
    return this.http.delete<Cart>(`/api/cart/items/${productId}`).pipe(tap((c) => this.state.set(c)));
  }

  clear(): Observable<Cart> {
    return this.http.delete<Cart>('/api/cart').pipe(tap((c) => this.state.set(c)));
  }

  reset(): void {
    this.state.set({ items: [], total: 0 });
  }
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  checkout(): Observable<Order> {
    return this.http.post<Order>('/api/orders/checkout', {});
  }

  findMine(): Observable<Order[]> {
    return this.http.get<Order[]>('/api/orders');
  }
}
