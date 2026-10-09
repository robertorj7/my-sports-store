import { discountPercent } from './pricing';

describe('discountPercent', () => {
  it('returns the rounded discount', () => {
    expect(discountPercent({ price: 499.9, effectivePrice: 399.9 })).toBe(20);
  });

  it('returns 0 without a discount', () => {
    expect(discountPercent({ price: 100, effectivePrice: 100 })).toBe(0);
    expect(discountPercent({ price: 0, effectivePrice: 0 })).toBe(0);
  });
});
