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

  protected addToCart(product: Product): void {
    const current = this.cartService.cart().items.find((i) => i.productId === product.id)?.quantity ?? 0;
    this.adding.set(product.id);
    this.message.set(null);
    this.error.set(null);

    this.cartService.setItem(product.id, current + 1).subscribe({
      next: () => {
        this.adding.set(null);
        this.message.set(`${product.name} adicionado ao carrinho.`);
      },
      error: (err: unknown) => {
        this.adding.set(null);
        this.error.set(errorMessage(err));
      },
    });
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
