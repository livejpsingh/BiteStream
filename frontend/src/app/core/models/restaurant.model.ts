export interface OperatingHoursDay {
  open: string;
  close: string;
}

export interface OperatingHours {
  monday?: OperatingHoursDay;
  tuesday?: OperatingHoursDay;
  wednesday?: OperatingHoursDay;
  thursday?: OperatingHoursDay;
  friday?: OperatingHoursDay;
  saturday?: OperatingHoursDay;
  sunday?: OperatingHoursDay;
  [day: string]: OperatingHoursDay | undefined;
}

export interface Restaurant {
  id: number;
  name: string;
  cuisineType: string;
  address: string;
  contactNumber: string;
  latitude: number;
  longitude: number;
  operatingHours: OperatingHours;
  isActive: boolean;
}

export interface MenuItem {
  id: number;
  restaurantId: number;
  name: string;
  description: string;
  price: number;
  prepTimeMinutes: number;
  isAvailable: boolean;
}
