import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Product } from '../../core/models';
import { Products } from './products';

registerLocaleData(localePt);

const product = (id: string, overrides: Partial<Product> = {}): Product => ({
  id,
  name: `Produto ${id}`,
  description: '',
  price: 100,
  promotionalPrice: null,
  effectivePrice: 100,
  onPromotion: false,
  image: `https://img/${id}`,
  color: '',
  category: 'Corrida',
  stock: 5,
  ...overrides,
});

describe('Products', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Products],
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

  async function setup(products: Product[], promotions: Product[]) {
    const fixture = TestBed.createComponent(Products);
    fixture.detectChanges();
    http.expectOne('/api/products/categories').flush(['Corrida']);
    http.expectOne('/api/products/promotions').flush(promotions);
    http.expectOne('/api/products').flush(products);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('shows the promotions carousel with the promotions from the API', async () => {
    const promo = product('p1', { promotionalPrice: 80, effectivePrice: 80, onPromotion: true });
    const el = await setup([promo, product('p2')], [promo]);

    const slides = el.querySelectorAll<HTMLAnchorElement>('app-promo-carousel .slide');
    expect(slides.length).toBe(1);
    expect(slides[0].getAttribute('href')).toBe('/products/p1');
  });

  it('hides the carousel when there are no promotions', async () => {
    const el = await setup([product('p1')], []);

    expect(el.querySelector('app-promo-carousel .carousel')).toBeNull();
  });

  it('shows the old price and discount badge only on cards on promotion', async () => {
    const promo = product('p1', { promotionalPrice: 75, effectivePrice: 75, onPromotion: true });
    const el = await setup([promo, product('p2')], [promo]);
    const [promoCard, regularCard] = Array.from(el.querySelectorAll('.grid .product'));

    expect(promoCard.querySelector('.old-price')!.textContent).toContain('100,00');
    expect(promoCard.querySelector('.discount-badge')!.textContent).toContain('-25%');
    expect(promoCard.querySelector('.price strong')!.textContent).toContain('75,00');
    expect(regularCard.querySelector('.old-price')).toBeNull();
    expect(regularCard.querySelector('.discount-badge')).toBeNull();
    expect(regularCard.querySelector('.price strong')!.textContent).toContain('100,00');
  });

  it('links product cards to the product page', async () => {
    const el = await setup([product('p1')], []);
    const card = el.querySelector('.grid .product')!;

    expect(card.querySelector('.image-link')!.getAttribute('href')).toBe('/products/p1');
    expect(card.querySelector('h2 a')!.getAttribute('href')).toBe('/products/p1');
  });

  it('keeps the carousel when filtering by category', async () => {
    const promo = product('p1', { promotionalPrice: 80, effectivePrice: 80, onPromotion: true });
    const el = await setup([promo], [promo]);

    Array.from(el.querySelectorAll<HTMLButtonElement>('.chip')).find((c) => c.textContent?.includes('Corrida'))!.click();
    http.expectOne((r) => r.url === '/api/products' && r.params.get('category') === 'Corrida').flush([]);
    http.expectNone('/api/products/promotions');

    expect(el.querySelectorAll('app-promo-carousel .slide').length).toBe(1);
  });
});
