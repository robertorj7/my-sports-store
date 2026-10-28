import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Order, OrderStatus } from '../../core/models';
import { Orders, PENDING_POLL_INTERVAL_MS } from './orders';

registerLocaleData(localePt);

function order(id: string, status: OrderStatus): Order {
  return {
    id: `${id}-0000000`,
    userId: 'user-1',
    items: [{ id: `${id}-i1`, productId: 'p1', name: 'Bola', price: 50, quantity: 2 }],
    total: 100,
    status,
    createdAt: '2026-10-24T12:00:00Z',
  };
}

describe('Orders', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [Orders],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'pt-BR' }],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  function setup(orders: Order[]) {
    const fixture = TestBed.createComponent(Orders);
    fixture.detectChanges();
    http.expectOne('/api/orders').flush(orders);
    fixture.detectChanges();
    return fixture;
  }

  it('refreshes while an order is pending and stops once payment settles', () => {
    const fixture = setup([order('o1', 'PENDING')]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.status')!.textContent).toContain('Pendente');
    expect(el.querySelector('.pending-hint')).not.toBeNull();

    vi.advanceTimersByTime(PENDING_POLL_INTERVAL_MS);
    http.expectOne('/api/orders').flush([order('o1', 'PAID')]);
    fixture.detectChanges();

    expect(el.querySelector('.status')!.textContent).toContain('Pago');
    expect(el.querySelector('.pending-hint')).toBeNull();
    vi.advanceTimersByTime(PENDING_POLL_INTERVAL_MS * 2);
    http.expectNone('/api/orders');
  });

  it('does not poll when no order is pending', () => {
    setup([order('o1', 'DELIVERED'), order('o2', 'CANCELLED')]);

    vi.advanceTimersByTime(PENDING_POLL_INTERVAL_MS * 2);
    http.expectNone('/api/orders');
  });

  it('stops polling when the page is destroyed', () => {
    const fixture = setup([order('o1', 'PENDING')]);

    fixture.destroy();
    vi.advanceTimersByTime(PENDING_POLL_INTERVAL_MS * 2);
    http.expectNone('/api/orders');
  });
});
