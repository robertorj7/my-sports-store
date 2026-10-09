import { CurrencyPipe, LocationStrategy } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../../core/models';
import { discountPercent } from '../../../core/pricing';

const MAX_SLIDES = 5;
const AUTOPLAY_MS = 5000;
const SWIPE_THRESHOLD = 50;

const THEMES = [
  'linear-gradient(120deg, #0086ff 0%, #0050c8 100%)',
  'linear-gradient(120deg, #ff3d6e 0%, #c3006b 100%)',
  'linear-gradient(120deg, #16a34a 0%, #0f6b32 100%)',
  'linear-gradient(120deg, #ff8a00 0%, #e3480b 100%)',
  'linear-gradient(120deg, #7c3aed 0%, #4c1d95 100%)',
];

@Component({
  selector: 'app-promo-carousel',
  imports: [CurrencyPipe],
  templateUrl: './promo-carousel.html',
  styleUrl: './promo-carousel.scss',
})
export class PromoCarousel {
  private readonly router = inject(Router);
  private readonly locationStrategy = inject(LocationStrategy);

  readonly products = input.required<Product[]>();

  protected readonly slides = computed(() => this.products().slice(0, MAX_SLIDES));
  protected readonly current = signal(0);
  protected readonly themes = THEMES;
  protected readonly discountPercent = discountPercent;

  private paused = false;
  private pointerStartX: number | null = null;
  private swiped = false;

  constructor() {
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (!reducedMotion) {
      const timer = setInterval(() => {
        if (!this.paused && !document.hidden) this.next();
      }, AUTOPLAY_MS);
      inject(DestroyRef).onDestroy(() => clearInterval(timer));
    }
  }

  protected goTo(index: number): void {
    const total = this.slides().length;
    if (total) this.current.set((index + total) % total);
  }

  protected next(): void {
    this.goTo(this.current() + 1);
  }

  protected prev(): void {
    this.goTo(this.current() - 1);
  }

  protected setPaused(paused: boolean): void {
    this.paused = paused;
  }

  protected onPointerDown(event: PointerEvent): void {
    this.pointerStartX = event.clientX;
    this.swiped = false;
  }

  protected onPointerUp(event: PointerEvent): void {
    if (this.pointerStartX === null) return;
    const delta = event.clientX - this.pointerStartX;
    this.pointerStartX = null;
    if (Math.abs(delta) < SWIPE_THRESHOLD) return;
    this.swiped = true;
    if (delta < 0) this.next();
    else this.prev();
  }

  protected productHref(product: Product): string {
    return this.locationStrategy.prepareExternalUrl(this.router.serializeUrl(this.productUrl(product)));
  }

  /**
   * Navigates like a RouterLink, except that the click ending a swipe is swallowed so dragging the
   * banner doesn't open the product (RouterLink would navigate even with the default prevented).
   */
  protected onSlideClick(event: MouseEvent, product: Product): void {
    if (this.swiped) {
      event.preventDefault();
      this.swiped = false;
      return;
    }
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    this.router.navigateByUrl(this.productUrl(product));
  }

  private productUrl(product: Product) {
    return this.router.createUrlTree(['/products', product.id]);
  }
}
