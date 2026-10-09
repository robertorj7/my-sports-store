import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Cart } from '../../core/models';
import { CartPage } from './cart';

registerLocaleData(localePt);

describe('CartPage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function setup(cart: Cart) {
    const fixture = TestBed.createComponent(CartPage);
    fixture.detectChanges();
    http.expectOne('/api/cart').flush(cart);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the original price struck through for items on promotion', async () => {
    const el = await setup({
      items: [
        { productId: 'p1', name: 'Bola', image: '', price: 80, originalPrice: 100, quantity: 2, lineTotal: 160 },
        { productId: 'p2', name: 'Raquete', image: '', price: 50, originalPrice: 50, quantity: 1, lineTotal: 50 },
      ],
      total: 210,
    });
    const [promoLine, regularLine] = Array.from(el.querySelectorAll('.line'));

    expect(promoLine.querySelector('.old-price')!.textContent).toContain('100,00');
    expect(promoLine.querySelector('.info .muted')!.textContent).toContain('80,00');
    expect(promoLine.querySelector('.line-total')!.textContent).toContain('160,00');
    expect(regularLine.querySelector('.old-price')).toBeNull();
    expect(el.querySelector('.total')!.textContent).toContain('210,00');
  });
});
