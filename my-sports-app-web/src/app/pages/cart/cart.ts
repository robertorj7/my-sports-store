import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { errorMessage } from '../../core/error-message';
import { Cart, CartLineItem } from '../../core/models';
import { CartService, OrderService } from '../../core/store.service';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class CartPage implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  protected readonly cart = this.cartService.cart;
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.run(this.cartService.load());
  }

  protected changeQuantity(item: CartLineItem, delta: number): void {
    const quantity = item.quantity + delta;
    this.run(quantity < 1 ? this.cartService.removeItem(item.productId) : this.cartService.setItem(item.productId, quantity));
  }

  protected remove(item: CartLineItem): void {
    this.run(this.cartService.removeItem(item.productId));
  }

  protected clear(): void {
    this.run(this.cartService.clear());
  }

  protected checkout(): void {
    this.busy.set(true);
    this.error.set(null);
    this.orderService.checkout().subscribe({
      next: () => {
        this.cartService.reset();
        this.router.navigate(['/orders']);
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  private run(action: Observable<Cart>): void {
    this.busy.set(true);
    this.error.set(null);
    action.subscribe({
      next: () => this.busy.set(false),
      error: (err: unknown) => {
        this.busy.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }
}
