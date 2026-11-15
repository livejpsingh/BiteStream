import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { Restaurant, MenuItem } from '../../core/models/restaurant.model';
import { OrderRequest, OrderResponse } from '../../core/models/order.model';

export interface CartItem {
  item: MenuItem;
  quantity: number;
}

@Component({
  selector: 'app-order-placement',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-placement.component.html',
  styleUrls: ['./order-placement.component.css']
})
export class OrderPlacementComponent {
  private api = inject(ApiService);

  @Input() selectedRestaurant: Restaurant | null = null;
  @Input() cartItems: CartItem[] = [];

  @Output() orderCreated = new EventEmitter<OrderResponse>();
  @Output() updateQuantity = new EventEmitter<{ item: MenuItem; delta: number }>();
  @Output() clearCart = new EventEmitter<void>();

  customerName = 'Rahul Verma';
  customerPhone = '+91-9876540001';
  customerAddress = 'Apartment 402, Lotus Towers, City Center';
  specialInstructions = 'Extra spicy, please do not ring the doorbell';
  paymentMethod = 'UPI_GPAY';

  // Distance selection (2.0 km, 3.0 km, 4.0 km, 5.0 km)
  distanceKm = 3.0;
  distanceOptions = [2.0, 3.0, 4.0, 5.0];

  loading = signal<boolean>(false);
  errorMessage = signal<string>('');

  get totalAmount(): number {
    return this.cartItems.reduce((acc, curr) => acc + (curr.item.price * curr.quantity), 0);
  }

  get maxPrepTime(): number {
    if (this.cartItems.length === 0) return 15;
    return Math.max(...this.cartItems.map(c => c.item.prepTimeMinutes));
  }

  get travelTimeMinutes(): number {
    // Formula: (distanceKm / 20 km/h * 60) + 4 min buffer
    return Math.round((this.distanceKm / 20.0) * 60 + 4);
  }

  get estimatedTotalEta(): number {
    // 1 order ahead in queue adds 3 minutes
    const queuePenalty = 1 * 3;
    return this.maxPrepTime + queuePenalty + this.travelTimeMinutes;
  }

  changeQuantity(item: MenuItem, delta: number) {
    this.updateQuantity.emit({ item, delta });
  }

  placeOrder() {
    if (this.cartItems.length === 0) {
      this.errorMessage.set('Please add at least one dish to the order.');
      return;
    }

    if (!this.selectedRestaurant) {
      this.errorMessage.set('Please select a restaurant.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const payload: OrderRequest = {
      customerName: this.customerName,
      customerPhone: this.customerPhone,
      customerAddress: this.customerAddress,
      restaurantId: this.selectedRestaurant.id,
      distanceKm: this.distanceKm,
      specialInstructions: this.specialInstructions,
      paymentMethod: this.paymentMethod,
      items: this.cartItems.map(c => ({
        menuItemId: c.item.id,
        quantity: c.quantity
      }))
    };

    this.api.createOrder(payload).subscribe({
      next: (order) => {
        this.loading.set(false);
        this.orderCreated.emit(order);
        this.clearCart.emit();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to place order.');
      }
    });
  }
}
