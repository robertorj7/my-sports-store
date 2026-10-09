import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Product } from '../../../core/models';
import { PromoCarousel } from './promo-carousel';

const promo = (id: string, price = 100, effectivePrice = 80): Product => ({
  id,
  name: `Produto ${id}`,
  description: '',
  price,
  promotionalPrice: effectivePrice,
  effectivePrice,
  onPromotion: true,
  image: `https://img/${id}`,
  color: '',
  category: 'Corrida',
  stock: 10,
});

registerLocaleData(localePt);

describe('PromoCarousel', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PromoCarousel],
      providers: [provideRouter([{ path: 'products/:id', children: [] }]), { provide: LOCALE_ID, useValue: 'pt-BR' }],
    });
  });

  afterEach(() => vi.useRealTimers());

  function setup(products: Product[]) {
    const fixture = TestBed.createComponent(PromoCarousel);
    fixture.componentRef.setInput('products', products);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el };
  }

  const activeIndex = (el: HTMLElement) =>
    Array.from(el.querySelectorAll('.dot')).findIndex((d) => d.classList.contains('active'));

  const click = (fixture: ComponentFixture<PromoCarousel>, selector: string) => {
    (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  };

  it('renders nothing when there are no promotions', () => {
    const { el } = setup([]);

    expect(el.querySelector('.carousel')).toBeNull();
  });

  it('renders at most 5 slides', () => {
    const { el } = setup(['1', '2', '3', '4', '5', '6', '7'].map((id) => promo(id)));

    expect(el.querySelectorAll('.slide').length).toBe(5);
    expect(el.querySelectorAll('.dot').length).toBe(5);
  });

  it('shows the discount, the regular price and the promotional price', () => {
    const { el } = setup([promo('1', 500, 400)]);
    const slide = el.querySelector('.slide')!;

    expect(slide.querySelector('.badge')!.textContent).toContain('20% OFF');
    expect(slide.querySelector('s')!.textContent).toContain('500,00');
    expect(slide.querySelector('.price strong')!.textContent).toContain('400,00');
    expect(slide.textContent).toContain('Produto 1');
  });

  it('links each slide to its product page', () => {
    const { el } = setup([promo('a'), promo('b')]);
    const links = Array.from(el.querySelectorAll<HTMLAnchorElement>('.slide')).map((a) => a.getAttribute('href'));

    expect(links).toEqual(['/products/a', '/products/b']);
  });

  it('navigates to the product page when the banner is clicked', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const { fixture, el } = setup([promo('a')]);

    el.querySelector<HTMLAnchorElement>('.slide')!.click();
    await fixture.whenStable();

    expect(navigate).toHaveBeenCalled();
    expect(TestBed.inject(Router).url).toBe('/products/a');
  });

  it('lets the browser handle ctrl+click so the product opens in a new tab', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const { el } = setup([promo('a')]);
    const click = new MouseEvent('click', { bubbles: true, cancelable: true, ctrlKey: true });

    el.querySelector('.slide')!.dispatchEvent(click);

    expect(click.defaultPrevented).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('hides arrows and dots when there is a single slide', () => {
    const { el } = setup([promo('1')]);

    expect(el.querySelector('.arrow')).toBeNull();
    expect(el.querySelector('.dots')).toBeNull();
  });

  it('moves between slides with the arrows, wrapping around', () => {
    const { fixture, el } = setup([promo('1'), promo('2'), promo('3')]);
    expect(activeIndex(el)).toBe(0);

    click(fixture, '.arrow.next');
    expect(activeIndex(el)).toBe(1);

    click(fixture, '.arrow.prev');
    click(fixture, '.arrow.prev');
    expect(activeIndex(el)).toBe(2);

    click(fixture, '.arrow.next');
    expect(activeIndex(el)).toBe(0);
  });

  it('jumps to a slide when its dot is clicked and moves the track', () => {
    const { fixture, el } = setup([promo('1'), promo('2'), promo('3')]);

    click(fixture, '.dot:nth-child(3)');

    expect(activeIndex(el)).toBe(2);
    expect(el.querySelector<HTMLElement>('.track')!.style.transform).toBe('translateX(-200%)');
  });

  it('makes only the visible slide focusable', () => {
    const { fixture, el } = setup([promo('1'), promo('2')]);
    click(fixture, '.arrow.next');
    const slides = Array.from(el.querySelectorAll<HTMLElement>('.slide'));

    expect(slides.map((s) => s.inert)).toEqual([true, false]);
    expect(slides.map((s) => s.getAttribute('aria-hidden'))).toEqual(['true', 'false']);
  });

  it('navigates with the keyboard arrows', () => {
    const { fixture, el } = setup([promo('1'), promo('2')]);
    const carousel = el.querySelector('.carousel')!;

    carousel.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(1);

    carousel.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(0);
  });

  it('advances automatically every 5 seconds', () => {
    vi.useFakeTimers();
    const { fixture, el } = setup([promo('1'), promo('2')]);

    vi.advanceTimersByTime(4999);
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(0);

    vi.advanceTimersByTime(1);
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(1);
  });

  it('pauses autoplay while the mouse is over the banner', () => {
    vi.useFakeTimers();
    const { fixture, el } = setup([promo('1'), promo('2')]);
    const carousel = el.querySelector('.carousel')!;

    carousel.dispatchEvent(new MouseEvent('mouseenter'));
    vi.advanceTimersByTime(10000);
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(0);

    carousel.dispatchEvent(new MouseEvent('mouseleave'));
    vi.advanceTimersByTime(5000);
    fixture.detectChanges();
    expect(activeIndex(el)).toBe(1);
  });

  it('stops autoplay when destroyed', () => {
    vi.useFakeTimers();
    const clear = vi.spyOn(globalThis, 'clearInterval');
    const { fixture } = setup([promo('1'), promo('2')]);

    fixture.destroy();

    expect(clear).toHaveBeenCalled();
  });

  describe('swipe', () => {
    const swipe = (fixture: ComponentFixture<PromoCarousel>, from: number, to: number) => {
      const track = (fixture.nativeElement as HTMLElement).querySelector('.track')!;
      track.dispatchEvent(new PointerEvent('pointerdown', { clientX: from }));
      track.dispatchEvent(new PointerEvent('pointerup', { clientX: to }));
      fixture.detectChanges();
    };

    it('goes to the next slide when swiping left and to the previous when swiping right', () => {
      const { fixture, el } = setup([promo('1'), promo('2'), promo('3')]);

      swipe(fixture, 300, 100);
      expect(activeIndex(el)).toBe(1);

      swipe(fixture, 300, 100);
      expect(activeIndex(el)).toBe(2);

      swipe(fixture, 100, 300);
      expect(activeIndex(el)).toBe(1);
    });

    it('ignores small movements', () => {
      const { fixture, el } = setup([promo('1'), promo('2')]);

      swipe(fixture, 100, 130);

      expect(activeIndex(el)).toBe(0);
    });

    it('does not open the product after a swipe', async () => {
      const router = TestBed.inject(Router);
      const { fixture, el } = setup([promo('1'), promo('2')]);
      swipe(fixture, 300, 100);

      el.querySelectorAll<HTMLElement>('.slide')[1].click();
      await fixture.whenStable();

      expect(router.url).toBe('/');
    });

    it('opens the product on a click that follows a swipe', async () => {
      const router = TestBed.inject(Router);
      const { fixture, el } = setup([promo('1'), promo('2')]);
      swipe(fixture, 300, 100);
      el.querySelectorAll<HTMLElement>('.slide')[1].click();
      await fixture.whenStable();

      el.querySelectorAll<HTMLElement>('.slide')[1].click();
      await fixture.whenStable();

      expect(router.url).toBe('/products/2');
    });
  });
});
