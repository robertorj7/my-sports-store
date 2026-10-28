import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { errorMessage } from '../../core/error-message';
import { Order, OrderStatus } from '../../core/models';
import { OrderService } from '../../core/store.service';

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pendente',
  PAID: 'Pago',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

/** Payment is confirmed asynchronously, so pending orders are refreshed until they settle. */
export const PENDING_POLL_INTERVAL_MS = 3000;

@Component({
  selector: 'app-orders',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './orders.html',
  styleUrl: './orders.scss',
})
export class Orders implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  private pollTimer?: ReturnType<typeof setTimeout>;

  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly statusLabels = STATUS_LABELS;

  ngOnInit(): void {
    this.destroyRef.onDestroy(() => clearTimeout(this.pollTimer));
    this.load();
  }

  private load(): void {
    this.orderService.findMine().subscribe({
      next: (orders) => {
        this.orders.set([...orders].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        this.loading.set(false);
        if (orders.some((order) => order.status === 'PENDING')) {
          this.pollTimer = setTimeout(() => this.load(), PENDING_POLL_INTERVAL_MS);
        }
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
