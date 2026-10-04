// Thin fetch wrappers around the three REST APIs the pages use.
const ORDER_API = import.meta.env.VITE_ORDER_API ?? "http://localhost:8081";
const INVENTORY_API = import.meta.env.VITE_INVENTORY_API ?? "http://localhost:8082";
const ANALYTICS_API = import.meta.env.VITE_ANALYTICS_API ?? "http://localhost:8085";

export class ApiError extends Error {
  constructor(status, message, fields) {
    super(message);
    this.status = status;
    this.fields = fields;
  }
}

async function request(url, options = {}) {
  let res;
  try {
    res = await fetch(url, {
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers ?? {}) },
    });
  } catch {
    throw new ApiError(0, `Cannot reach ${new URL(url).origin}. Is that service running?`);
  }
  const body = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) {
    // Spring returns RFC 9457 problem details: { title, status, detail, fields? }
    throw new ApiError(res.status, body?.detail ?? body?.title ?? `Request failed (${res.status})`, body?.fields);
  }
  return body;
}

export const api = {
  products: () => request(`${INVENTORY_API}/api/products`),
  createProduct: (p) => request(`${INVENTORY_API}/api/products`, { method: "POST", body: JSON.stringify(p) }),
  updateProduct: (id, changes) =>
    request(`${INVENTORY_API}/api/products/${id}`, { method: "PUT", body: JSON.stringify(changes) }),

  placeOrder: (order) => request(`${ORDER_API}/api/orders`, { method: "POST", body: JSON.stringify(order) }),
  order: (id) => request(`${ORDER_API}/api/orders/${id}`),
  recentOrders: (customerId) => request(`${ORDER_API}/api/orders?customerId=${customerId}`),
  orderEvents: (id) => new EventSource(`${ORDER_API}/api/orders/${id}/events`),

  topProducts: () => request(`${ANALYTICS_API}/api/analytics/top-products`),
};

export const rupees = (n) =>
  "₹" + Number(n).toLocaleString("en-IN", { minimumFractionDigits: 0, maximumFractionDigits: 2 });
