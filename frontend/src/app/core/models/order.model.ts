export interface OrderItemRequest {
  menuItemId: number;
  quantity: number;
}

export interface OrderRequest {
  customerName: string;
  customerPhone: string;
  customerAddress: string;
  restaurantId: number;
  distanceKm: number;
  specialInstructions?: string;
  paymentMethod?: string;
  items: OrderItemRequest[];
}

export interface LifecycleEvent {
  eventType: string;
  topic: string;
  timestamp: string;
  description: string;
  triggeredBy: string;
}

export interface OrderResponse {
  orderId: number;
  orderNumber: string;
  status: 'PLACED' | 'PREPARING' | 'PICKED_UP' | 'DELIVERED' | 'CANCELLED';
  restaurantId: number;
  restaurantName: string;
  customerName: string;
  distanceKm: number;
  queuePosition: number;
  estimatedPrepTimeMinutes: number;
  estimatedTravelTimeMinutes: number;
  estimatedTotalDeliveryTimeMinutes: number;
  totalAmount: number;
  metadata?: {
    specialInstructions?: string;
    paymentMethod?: string;
    priority?: string;
    [key: string]: any;
  };
  lifecycleEvents?: LifecycleEvent[];
  deliveryAgentName?: string;
  assignedAgentVehicle?: string;
  activePickupOtpInRedis?: string;
  activeDeliveryOtpInRedis?: string;
}
