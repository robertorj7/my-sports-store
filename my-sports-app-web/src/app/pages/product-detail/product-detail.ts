import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../core/error-message';
import { Product } from '../../core/models';
import { discountPercent } from '../../core/pricing';
import { CartService, ProductService } from '../../core/store.service';

@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss',
})
export class ProductDetail {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);

  /** Bound from the `:id` route param. */
  readonly id = input.required<string>();

  protected readonly product = signal<Product | null>(null);
  protected readonly loading = signal(true);
  protected readonly adding = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly quantity = signal(1);
  protected readonly discountPercent = discountPercent;

  protected readonly inCart = computed(
    () => this.cartService.cart().items.find((i) => i.productId === this.product()?.id)?.quantity ?? 0,
  );
  protected readonly max = computed(() => Math.max(1, (this.product()?.stock ?? 0) - this.inCart()));

  constructor() {
    effect(() => this.load(this.id()));
  }

  protected changeQuantity(delta: number): void {
    this.setQuantity(this.quantity() + delta);
  }

  protected setQuantity(value: number): void {
    this.quantity.set(Math.min(Math.max(1, Math.floor(value) || 1), this.max()));
  }

  protected addToCart(product: Product): void {
    const quantity = this.quantity();
    this.adding.set(true);
    this.message.set(null);
    this.error.set(null);

    this.cartService.setItem(product.id, this.inCart() + quantity).subscribe({
      next: () => {
        this.adding.set(false);
        this.quantity.set(1);
        this.message.set(`${quantity}x ${product.name} adicionado(s) ao carrinho.`);
      },
      error: (err: unknown) => {
        this.adding.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  private load(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.quantity.set(1);
    this.productService.findById(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.product.set(null);
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
