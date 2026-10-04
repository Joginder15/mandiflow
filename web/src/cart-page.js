import { api, rupees } from "./api.js";
import { escapeHtml, mountHeader, produceTile, toast } from "./layout.js";
import { cart, customer } from "./store.js";

mountHeader("cart");
const app = document.querySelector("#app");

function render() {
  const lines = cart.lines();
  if (lines.length === 0) {
    app.innerHTML = `
      <div class="page-head"><h1>Your cart</h1></div>
      <div class="card empty">Your cart is empty. <a href="/index.html">Browse the catalogue</a>.</div>`;
    return;
  }
  app.innerHTML = `
    <div class="page-head">
      <h1>Your cart</h1>
      <p class="muted">Ordering as customer #${customer.id()}. Orders above ₹2,000 are declined by the fake payment gateway.</p>
    </div>
    <div class="card">
      <ul class="cart-lines">
        ${lines.map((l) => `
          <li class="cart-line" data-id="${l.productId}">
            ${produceTile(l.name)}
            <div><strong>${escapeHtml(l.name)}</strong><br><span class="hint">${rupees(l.unitPrice)} per ${escapeHtml(l.unit)}</span></div>
            <div class="stepper">
              <button type="button" data-step="-1" aria-label="Less">−</button>
              <input type="number" min="0" max="100" value="${l.quantity}" aria-label="Quantity of ${escapeHtml(l.name)}">
              <button type="button" data-step="1" aria-label="More">+</button>
            </div>
            <span class="line-total">${rupees(l.unitPrice * l.quantity)}</span>
          </li>`).join("")}
      </ul>
    </div>
    <div class="card summary">
      <div><span class="hint">Total</span><div class="total">${rupees(cart.total())}</div></div>
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn ghost" id="clear">Empty cart</button>
        <button class="btn" id="place">Place order</button>
      </div>
    </div>`;

  app.querySelectorAll(".cart-line").forEach((row) => {
    const id = Number(row.dataset.id);
    const input = row.querySelector("input");
    const set = (n) => { cart.setQuantity(id, Math.max(0, Math.min(100, n))); render(); };
    row.querySelectorAll("[data-step]").forEach((b) =>
      b.addEventListener("click", () => set(Number(input.value) + Number(b.dataset.step))));
    input.addEventListener("change", () => set(Number.parseInt(input.value, 10) || 0));
  });
  app.querySelector("#clear").addEventListener("click", () => { cart.clear(); render(); });
  app.querySelector("#place").addEventListener("click", placeOrder);
}

async function placeOrder(event) {
  const button = event.currentTarget;
  button.disabled = true;
  button.textContent = "Placing…";
  try {
    const order = await api.placeOrder({
      customerId: customer.id(),
      lines: cart.lines().map((l) => ({ productId: l.productId, quantity: l.quantity, unitPrice: l.unitPrice })),
    });
    cart.clear();
    location.href = `/track.html?id=${order.id}`;
  } catch (err) {
    const message = err.status === 429 ? `Slow down: ${err.message}.` : err.message;
    toast(message, "error");
    button.disabled = false;
    button.textContent = "Place order";
  }
}

render();
