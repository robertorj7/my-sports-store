import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CartService } from '../core/store.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly cartService = inject(CartService);

  protected readonly cart = this.cartService.cart;

  ngOnInit(): void {
    this.cartService.load().subscribe({ error: () => {} });
  }

  protected itemCount(): number {
    return this.cart().items.reduce((sum, item) => sum + item.quantity, 0);
  }

  protected logout(): void {
    this.cartService.reset();
    this.auth.logout();
  }
}
