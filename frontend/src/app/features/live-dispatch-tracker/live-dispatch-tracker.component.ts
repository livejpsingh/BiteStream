import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { OrderResponse } from '../../core/models/order.model';
import { DeliveryAgent } from '../../core/models/delivery.model';

@Component({
  selector: 'app-live-dispatch-tracker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './live-dispatch-tracker.component.html',
  styleUrls: ['./live-dispatch-tracker.component.css']
})
export class LiveDispatchTrackerComponent implements OnInit {
  private api = inject(ApiService);

  @Input() currentOrder: OrderResponse | null = null;
  @Output() openOtpModal = new EventEmitter<{ orderId: number; orderNumber: string; stepType: 'PICKUP' | 'DELIVERY'; activeOtp: string }>();

  deliveryAgents = signal<DeliveryAgent[]>([]);
  activeTab = signal<'LIFECYCLE' | 'FLEET' | 'KAFKA'>('LIFECYCLE');

  ngOnInit() {
    this.api.getDeliveryAgents().subscribe({
      next: (agents) => this.deliveryAgents.set(agents)
    });
  }

  triggerPickupOtp() {
    if (!this.currentOrder) return;
    this.openOtpModal.emit({
      orderId: this.currentOrder.orderId,
      orderNumber: this.currentOrder.orderNumber,
      stepType: 'PICKUP',
      activeOtp: this.currentOrder.activePickupOtpInRedis || '684920'
    });
  }

  triggerDeliveryOtp() {
    if (!this.currentOrder) return;
    this.openOtpModal.emit({
      orderId: this.currentOrder.orderId,
      orderNumber: this.currentOrder.orderNumber,
      stepType: 'DELIVERY',
      activeOtp: this.currentOrder.activeDeliveryOtpInRedis || '512394'
    });
  }
}
