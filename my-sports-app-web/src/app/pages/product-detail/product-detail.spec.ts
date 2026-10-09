import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Product } from '../../core/models';
import { ProductDetail } from './product-detail';

registerLocaleData(localePt);

const product = (overrides: Partial<Product> = {}): Product => ({
  id: 'p1',
  name: 'Tênis de Corrida',
  description: 'Leve e responsivo',
  price: 500,
  promotionalPrice: null,
  effectivePrice: 500,
  onPromotion: false,
  image: 'https://img/p1',
  color: 'Vermelho',
  category: 'Corrida',
  stock: 3,
  ...overrides,
});

describe('ProductDetail', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProductDetail],
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

  async function setup(response: Product) {
    const fixture = TestBed.createComponent(ProductDetail);
    fixture.componentRef.setInput('id', response.id);
    fixture.detectChanges();
    http.expectOne(`/api/products/${response.id}`).flush(response);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('loads the product from the route id', async () => {
    const { el } = await setup(product());

    expect(el.querySelector('h1')!.textContent).toContain('Tênis de Corrida');
    expect(el.textContent).toContain('Leve e responsivo');
    expect(el.textContent).toContain('3 em estoque');
  });

  it('shows only the regular price when not on promotion', async () => {
    const { el } = await setup(product());

    expect(el.querySelector('.price strong')!.textContent).toContain('500,00');
    expect(el.querySelector('.old-price')).toBeNull();
    expect(el.querySelector('.discount-badge')).toBeNull();
  });

  it('shows the old price, the discount and the promotional price when on promotion', async () => {
    const { el } = await setup(product({ promotionalPrice: 400, effectivePrice: 400, onPromotion: true }));

    expect(el.querySelector('.old-price')!.textContent).toContain('500,00');
    expect(el.querySelector('.discount-badge')!.textContent).toContain('-20%');
    expect(el.querySelector('.price strong')!.textContent).toContain('400,00');
  });

  it('adds the selected quantity to the cart', async () => {
    const { fixture, el } = await setup(product({ promotionalPrice: 400, effectivePrice: 400, onPromotion: true }));

    el.querySelector<HTMLButtonElement>('[aria-label="Aumentar quantidade"]')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.btn-primary')!.click();

    const req = http.expectOne('/api/cart/items');
    expect(req.request.body).toEqual({ productId: 'p1', quantity: 2 });
    req.flush({ items: [], total: 800 });
    fixture.detectChanges();

    expect(el.querySelector('.alert-success')!.textContent).toContain('2x Tênis de Corrida');
  });

  it('does not allow buying more than the stock', async () => {
    const { fixture, el } = await setup(product({ stock: 2 }));
    const increase = el.querySelector<HTMLButtonElement>('[aria-label="Aumentar quantidade"]')!;

    increase.click();
    fixture.detectChanges();

    expect(increase.disabled).toBe(true);
  });

  it('disables buying when out of stock', async () => {
    const { el } = await setup(product({ stock: 0 }));
    const button = el.querySelector<HTMLButtonElement>('.btn-primary')!;

    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Esgotado');
  });

  it('shows the API error when the product does not exist', async () => {
    const fixture = TestBed.createComponent(ProductDetail);
    fixture.componentRef.setInput('id', 'missing');
    fixture.detectChanges();
    http
      .expectOne('/api/products/missing')
      .flush({ message: 'Product not found: missing' }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('.alert-error')!.textContent).toContain('Product not found: missing');
    expect(el.querySelector('h1')).toBeNull();
  });
});
