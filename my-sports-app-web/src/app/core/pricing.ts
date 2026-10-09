import { Product } from './models';

/** Discount percentage, rounded, or 0 when the product is not on promotion. */
export function discountPercent(product: Pick<Product, 'price' | 'effectivePrice'>): number {
  if (!product.price || product.effectivePrice >= product.price) return 0;
  return Math.round((1 - product.effectivePrice / product.price) * 100);
}
