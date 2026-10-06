import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { errorMessage } from '../../core/error-message';
import { Product } from '../../core/models';
import { CartService, ProductService } from '../../core/store.service';

@Component({
  selector: 'app-products',
  imports: [CurrencyPipe],
  templateUrl: './products.html',
  styleUrl: './products.scss',
})
export class Products implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly searchInput = new Subject<string>();

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<string[]>([]);
  protected readonly category = signal('');
  protected readonly search = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly adding = signal<string | null>(null);
  protected readonly quantities = signal<Record<string, number>>({});

  constructor() {
    this.searchInput.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((term) => {
      this.search.set(term);
      this.load();
    });
  }

  ngOnInit(): void {
    this.productService.categories().subscribe({ next: (c) => this.categories.set(c) });
    this.load();
  }

  protected onSearch(term: string): void {
    this.searchInput.next(term.trim());
  }

  protected selectCategory(category: string): void {
    this.category.set(category);
    this.load();
  }

  protected quantityOf(product: Product): number {
    return this.quantities()[product.id] ?? 1;
  }

  protected maxOf(product: Product): number {
    return Math.max(1, product.stock - this.inCart(product));
  }

  protected changeQuantity(product: Product, delta: number): void {
    this.setQuantity(product, this.quantityOf(product) + delta);
  }

  protected setQuantity(product: Product, value: number): void {
    const quantity = Math.min(Math.max(1, Math.floor(value) || 1), this.maxOf(product));
    this.quantities.update((q) => ({ ...q, [product.id]: quantity }));
  }

  protected addToCart(product: Product): void {
    const quantity = this.quantityOf(product);
    this.adding.set(product.id);
    this.message.set(null);
    this.error.set(null);

    this.cartService.setItem(product.id, this.inCart(product) + quantity).subscribe({
      next: () => {
        this.adding.set(null);
        this.quantities.update((q) => ({ ...q, [product.id]: 1 }));
        this.message.set(`${quantity}x ${product.name} adicionado(s) ao carrinho.`);
      },
      error: (err: unknown) => {
        this.adding.set(null);
        this.error.set(errorMessage(err));
      },
    });
  }

  private inCart(product: Product): number {
    return this.cartService.cart().items.find((i) => i.productId === product.id)?.quantity ?? 0;
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.productService.findAll({ category: this.category(), search: this.search() }).subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
