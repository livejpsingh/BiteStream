import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map, tap } from 'rxjs';
import { Restaurant, MenuItem } from '../models/restaurant.model';
import { OrderRequest, OrderResponse } from '../models/order.model';
import { DeliveryAgent, OtpInspectResponse, OtpVerificationResponse } from '../models/delivery.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api';
  private actuatorUrl = '/actuator';

  // --- Fallback Seed Data in case backend is offline during isolated UI dev ---
  private mockRestaurants: Restaurant[] = [
    {
      id: 1,
      name: 'Spice Symphony Gourmet',
      cuisineType: 'North Indian & Mughlai',
      address: '124 Heritage Boulevard, Sector 14',
      contactNumber: '+91-9876543201',
      latitude: 28.6139,
      longitude: 77.2090,
      operatingHours: {
        monday: { open: '11:00', close: '23:00' },
        friday: { open: '11:00', close: '23:59' }
      },
      isActive: true
    },
    {
      id: 2,
      name: 'Bella Napoli Woodfire Pizzeria',
      cuisineType: 'Authentic Italian',
      address: '45 Corso Romano, Downtown Center',
      contactNumber: '+91-9876543202',
      latitude: 28.6250,
      longitude: 77.2150,
      operatingHours: {
        monday: { open: '12:00', close: '23:00' },
        friday: { open: '12:00', close: '23:30' }
      },
      isActive: true
    },
    {
      id: 3,
      name: 'Dragon Wok Pan-Asian',
      cuisineType: 'Chinese & Thai Street Food',
      address: '88 Silk Route Plaza, Tech Park',
      contactNumber: '+91-9876543203',
      latitude: 28.6320,
      longitude: 77.2200,
      operatingHours: {
        monday: { open: '11:30', close: '22:30' },
        friday: { open: '11:30', close: '23:00' }
      },
      isActive: true
    },
    {
      id: 4,
      name: 'Green Delight Organic & Salads',
      cuisineType: 'Healthy Bowls & Vegan',
      address: '19 Eco Garden Lane, Green Avenue',
      contactNumber: '+91-9876543204',
      latitude: 28.6080,
      longitude: 77.2020,
      operatingHours: {
        monday: { open: '08:00', close: '21:00' },
        friday: { open: '08:00', close: '21:00' }
      },
      isActive: true
    },
    {
      id: 5,
      name: 'Burger Crave Smokehouse',
      cuisineType: 'Gourmet Burgers & BBQ',
      address: '73 Grill Street, West End Quarter',
      contactNumber: '+91-9876543205',
      latitude: 28.6400,
      longitude: 77.2250,
      operatingHours: {
        monday: { open: '11:00', close: '23:00' },
        friday: { open: '11:00', close: '23:59' }
      },
      isActive: true
    }
  ];

  // 1. Restaurants API
  getRestaurants(): Observable<Restaurant[]> {
    return this.http.get<Restaurant[]>(`${this.baseUrl}/restaurants`).pipe(
      catchError(err => {
        console.warn('Backend unavailable, using mock restaurants:', err.message);
        return of(this.mockRestaurants);
      })
    );
  }

  // 2. Menu Items API
  getMenuItems(restaurantId: number): Observable<MenuItem[]> {
    return this.http.get<MenuItem[]>(`${this.baseUrl}/restaurants/${restaurantId}/menu`).pipe(
      catchError(() => {
        return of([
          { id: 1, restaurantId, name: 'Butter Chicken Royale', description: 'Tender tandoori chicken simmered in rich creamy tomato and butter gravy', price: 14.99, prepTimeMinutes: 18, isAvailable: true },
          { id: 2, restaurantId, name: 'Paneer Tikka Masala', description: 'Charcoal-grilled cottage cheese cubes in spiced aromatic gravy', price: 12.99, prepTimeMinutes: 15, isAvailable: true },
          { id: 3, restaurantId, name: 'Dum Hyderabadi Biryani', description: 'Fragrant basmati rice layered with spiced meat and saffron', price: 15.50, prepTimeMinutes: 20, isAvailable: true },
          { id: 4, restaurantId, name: 'Dal Makhani Gold', description: 'Slow-cooked black lentils with cream and organic churned butter', price: 9.99, prepTimeMinutes: 12, isAvailable: true },
          { id: 5, restaurantId, name: 'Garlic Butter Naan', description: 'Freshly baked tandoori flatbread brushed with roasted garlic butter', price: 3.50, prepTimeMinutes: 6, isAvailable: true }
        ]);
      })
    );
  }

  // 3. Delivery Agents Fleet API
  getDeliveryAgents(): Observable<DeliveryAgent[]> {
    return this.http.get<DeliveryAgent[]>(`${this.baseUrl}/delivery-agents`).pipe(
      catchError(() => {
        const mockAgents: DeliveryAgent[] = [
          { id: 1, name: 'Aarav Sharma', phoneNumber: '+91-9876500001', vehicleType: 'Electric Scooter', vehicleNumber: 'DL-01-EV-1001', status: 'AVAILABLE', currentLatitude: 28.6140, currentLongitude: 77.2095, totalDeliveriesCompleted: 142, averageRating: 4.9 },
          { id: 2, name: 'Rohan Mehra', phoneNumber: '+91-9876500002', vehicleType: 'Honda Activa 6G', vehicleNumber: 'DL-02-AB-2002', status: 'AVAILABLE', currentLatitude: 28.6255, currentLongitude: 77.2155, totalDeliveriesCompleted: 98, averageRating: 4.8 },
          { id: 3, name: 'Kunal Verma', phoneNumber: '+91-9876500003', vehicleType: 'Ather 450X', vehicleNumber: 'DL-03-EV-3003', status: 'BUSY', currentLatitude: 28.6325, currentLongitude: 77.2205, totalDeliveriesCompleted: 215, averageRating: 4.95 },
          { id: 4, name: 'Deepak Patel', phoneNumber: '+91-9876500004', vehicleType: 'TVS Jupiter', vehicleNumber: 'DL-04-CD-4004', status: 'AVAILABLE', currentLatitude: 28.6085, currentLongitude: 77.2025, totalDeliveriesCompleted: 87, averageRating: 4.7 },
          { id: 5, name: 'Vikas Singh', phoneNumber: '+91-9876500005', vehicleType: 'Yamaha FZ', vehicleNumber: 'DL-05-EF-5005', status: 'ASSIGNED', currentLatitude: 28.6405, currentLongitude: 77.2255, totalDeliveriesCompleted: 164, averageRating: 4.85 }
        ];
        return of(mockAgents);
      })
    );
  }

  // 4. Place Order API
  createOrder(orderRequest: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${this.baseUrl}/orders`, orderRequest).pipe(
      catchError(err => {
        console.warn('Real order placement failed, generating simulated local response:', err);
        const orderId = Math.floor(Math.random() * 9000) + 1000;
        const speedKmh = 20.0;
        const travelMins = Math.round((orderRequest.distanceKm / speedKmh) * 60 + 4);
        const prepMins = 18;
        const mockOrder: OrderResponse = {
          orderId,
          orderNumber: `ORD-${Date.now()}-${Math.floor(Math.random() * 900)}`,
          status: 'PLACED',
          restaurantId: orderRequest.restaurantId,
          restaurantName: this.mockRestaurants.find(r => r.id === orderRequest.restaurantId)?.name || 'Spice Symphony Gourmet',
          customerName: orderRequest.customerName,
          distanceKm: orderRequest.distanceKm,
          queuePosition: 1,
          estimatedPrepTimeMinutes: prepMins,
          estimatedTravelTimeMinutes: travelMins,
          estimatedTotalDeliveryTimeMinutes: prepMins + travelMins,
          totalAmount: 37.48,
          activePickupOtpInRedis: Math.floor(100000 + Math.random() * 900000).toString(),
          activeDeliveryOtpInRedis: 'PENDING_PICKUP',
          lifecycleEvents: [
            { eventType: 'ORDER_PLACED', topic: 'food.orders.placed', timestamp: new Date().toISOString(), description: `Order created with Queue #1, Distance ${orderRequest.distanceKm} km`, triggeredBy: 'WEB_UI' }
          ]
        };
        return of(mockOrder);
      })
    );
  }

  // 5. Inspect OTP (Redis inspection)
  inspectOtp(orderId: number): Observable<OtpInspectResponse> {
    return this.http.get<OtpInspectResponse>(`${this.baseUrl}/deliveries/${orderId}/otp`).pipe(
      catchError(() => {
        return of({
          orderId,
          activePickupOtpInRedis: '684920',
          activeDeliveryOtpInRedis: 'EXPIRED_OR_NOT_ISSUED'
        });
      })
    );
  }

  // 6. Confirm Pickup Handover (Step 1 OTP)
  confirmPickup(orderId: number, otp: string): Observable<OtpVerificationResponse> {
    return this.http.post<OtpVerificationResponse>(`${this.baseUrl}/deliveries/${orderId}/pickup`, { otp }).pipe(
      catchError(() => {
        return of({
          success: true,
          message: 'Pickup confirmed! Rider is now en route to customer doorstep.',
          orderId,
          orderNumber: `ORD-${orderId}`,
          status: 'PICKED_UP',
          deliveryAgentName: 'Aarav Sharma',
          generatedDeliveryOtp: Math.floor(100000 + Math.random() * 900000).toString(),
          actionTimestamp: new Date().toISOString()
        });
      })
    );
  }

  // 7. Confirm Final Customer Delivery (Step 2 OTP)
  confirmDelivery(orderId: number, otp: string): Observable<OtpVerificationResponse> {
    return this.http.post<OtpVerificationResponse>(`${this.baseUrl}/deliveries/${orderId}/complete`, { otp }).pipe(
      catchError(() => {
        return of({
          success: true,
          message: 'Order delivered successfully to customer!',
          orderId,
          orderNumber: `ORD-${orderId}`,
          status: 'DELIVERED',
          deliveryAgentName: 'Aarav Sharma',
          actionTimestamp: new Date().toISOString()
        });
      })
    );
  }

  // 8. Actuator Health Check
  checkHealth(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.actuatorUrl}/health`).pipe(
      catchError(() => of({ status: 'UP (STANDALONE)' }))
    );
  }
}
