export interface AuthResponse {
  token: string;
  email: string;
  name: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  image: string;
  color: string;
  category: string;
  stock: number;
  promotionalPrice: number | null;
  /** Price actually charged: the promotional price when on promotion, otherwise the regular price. */
  effectivePrice: number;
  onPromotion: boolean;
}

export interface CartLineItem {
  productId: string;
  name: string;
  image: string;
  price: number;
  originalPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Cart {
  items: CartLineItem[];
  total: number;
}

export type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: string;
  productId: string;
  name: string;
  price: number;
  quantity: number;
}

export interface Order {
  id: string;
  userId: string;
  items: OrderItem[];
  total: number;
  status: OrderStatus;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  fieldErrors?: Record<string, string>;
}
