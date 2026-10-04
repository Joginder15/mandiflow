// Per-browser conveniences: the cart and which demo customer you are.
const CART_KEY = "mandiflow-cart";
const CUSTOMER_KEY = "mandiflow-customer";

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* private mode: the cart simply won't survive a reload */
  }
  window.dispatchEvent(new CustomEvent("mandiflow:change"));
}

// Cart lines: { productId, name, unit, unitPrice, quantity }
export const cart = {
  lines: () => read(CART_KEY, []),
  count: () => cart.lines().reduce((n, l) => n + l.quantity, 0),
  total: () => cart.lines().reduce((t, l) => t + l.unitPrice * l.quantity, 0),
  add(product, quantity) {
    const lines = cart.lines();
    const line = lines.find((l) => l.productId === product.id);
    if (line) {
      line.quantity += quantity;
      line.unitPrice = Number(product.pricePerUnit);
    } else {
      lines.push({
        productId: product.id,
        name: product.name,
        unit: product.unit,
        unitPrice: Number(product.pricePerUnit),
        quantity,
      });
    }
    write(CART_KEY, lines);
  },
  setQuantity(productId, quantity) {
    const lines = cart.lines()
      .map((l) => (l.productId === productId ? { ...l, quantity } : l))
      .filter((l) => l.quantity > 0);
    write(CART_KEY, lines);
  },
  clear: () => write(CART_KEY, []),
};

export const customer = {
  id: () => read(CUSTOMER_KEY, 42),
  set: (id) => write(CUSTOMER_KEY, id),
};
