export interface DeliveryAgent {
  id: number;
  name: string;
  phoneNumber: string;
  vehicleType: string;
  vehicleNumber: string;
  status: 'AVAILABLE' | 'ASSIGNED' | 'BUSY' | 'OFFLINE';
  currentLatitude: number;
  currentLongitude: number;
  totalDeliveriesCompleted: number;
  averageRating: number;
}

export interface OtpInspectResponse {
  orderId: number;
  activePickupOtpInRedis: string;
  activeDeliveryOtpInRedis: string;
}

export interface OtpVerificationRequest {
  otp: string;
}

export interface OtpVerificationResponse {
  success: boolean;
  message: string;
  orderId: number;
  orderNumber: string;
  status: string;
  deliveryAgentName: string;
  generatedDeliveryOtp?: string;
  actionTimestamp: string;
}
