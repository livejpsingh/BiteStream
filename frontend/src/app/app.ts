import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { StatsBarComponent } from './shared/components/stats-bar/stats-bar.component';
import { OtpModalComponent } from './shared/components/otp-modal/otp-modal.component';
import { RestaurantCatalogComponent } from './features/restaurant-catalog/restaurant-catalog.component';
import { OrderPlacementComponent, CartItem } from './features/order-placement/order-placement.component';
import { LiveDispatchTrackerComponent } from './features/live-dispatch-tracker/live-dispatch-tracker.component';
import { Restaurant, MenuItem } from './core/models/restaurant.model';
import { OrderResponse } from './core/models/order.model';
import { OtpVerificationResponse } from './core/models/delivery.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    NavbarComponent,
    StatsBarComponent,
    OtpModalComponent,
    RestaurantCatalogComponent,
    OrderPlacementComponent,
    LiveDispatchTrackerComponent
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App {
  title = 'BiteStream - Food Delivery Platform';

  selectedRestaurant = signal<Restaurant | null>(null);
  cartItems = signal<CartItem[]>([]);
  activeOrder = signal<OrderResponse | null>(null);

  // OTP Modal State
  otpModalOpen = signal<boolean>(false);
  modalOrderId = signal<number>(0);
  modalOrderNumber = signal<string>('');
  modalStepType = signal<'PICKUP' | 'DELIVERY'>('PICKUP');
  modalActiveOtp = signal<string>('');

  onDishSelected(event: { restaurant: Restaurant; item: MenuItem }) {
    if (this.selectedRestaurant()?.id !== event.restaurant.id) {
      this.selectedRestaurant.set(event.restaurant);
      this.cartItems.set([{ item: event.item, quantity: 1 }]);
    } else {
      const items = [...this.cartItems()];
      const existing = items.find(i => i.item.id === event.item.id);
      if (existing) {
        existing.quantity += 1;
      } else {
        items.push({ item: event.item, quantity: 1 });
      }
      this.cartItems.set(items);
    }
  }

  onUpdateQuantity(event: { item: MenuItem; delta: number }) {
    const items = [...this.cartItems()];
    const index = items.findIndex(i => i.item.id === event.item.id);
    if (index > -1) {
      items[index].quantity += event.delta;
      if (items[index].quantity <= 0) {
        items.splice(index, 1);
      }
      this.cartItems.set(items);
    }
  }

  onClearCart() {
    this.cartItems.set([]);
  }

  onOrderPlaced(order: OrderResponse) {
    this.activeOrder.set(order);
  }

  onOpenOtpModal(event: { orderId: number; orderNumber: string; stepType: 'PICKUP' | 'DELIVERY'; activeOtp: string }) {
    this.modalOrderId.set(event.orderId);
    this.modalOrderNumber.set(event.orderNumber);
    this.modalStepType.set(event.stepType);
    this.modalActiveOtp.set(event.activeOtp);
    this.otpModalOpen.set(true);
  }

  onOtpConfirmed(response: OtpVerificationResponse) {
    const curr = this.activeOrder();
    if (curr && curr.orderId === response.orderId) {
      const updated: OrderResponse = {
        ...curr,
        status: response.status as any,
        deliveryAgentName: response.deliveryAgentName || curr.deliveryAgentName,
        activeDeliveryOtpInRedis: response.generatedDeliveryOtp || curr.activeDeliveryOtpInRedis
      };
      this.activeOrder.set(updated);
    }
  }
}
