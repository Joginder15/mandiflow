import { api, rupees } from "./api.js";
import { escapeHtml, mountHeader, produceTile, toast } from "./layout.js";
import { cart } from "./store.js";

mountHeader("catalogue");
const app = document.querySelector("#app");
app.innerHTML = `
  <div class="page-head">
    <h1>Fresh from the mandi</h1>
    <p class="muted">Today's prices. Add produce to your cart and watch the order travel through Kafka.</p>
  </div>
  <div class="grid" id="grid"><p class="muted">Loading the catalogue…</p></div>`;
const grid = app.querySelector("#grid");

function stockLabel(p) {
  if (p.availableQty <= 0) return `<span class="stock out">Sold out</span>`;
  if (p.availableQty < 20) return `<span class="stock low">Only ${p.availableQty} ${escapeHtml(p.unit)} left</span>`;
  return `<span class="stock">${p.availableQty} ${escapeHtml(p.unit)} in stock</span>`;
}

function render(products) {
  if (products.length === 0) {
    grid.innerHTML = `<div class="card empty">No produce yet. Add some on the Admin page.</div>`;
    return;
  }
  grid.innerHTML = products.map((p) => `
    <article class="card product" data-id="${p.id}">
      <div class="product-top">
        ${produceTile(p.name)}
        <div>
          <h2>${escapeHtml(p.name)}</h2>
          ${stockLabel(p)}
        </div>
      </div>
      <div class="price">${rupees(p.pricePerUnit)} <small>per ${escapeHtml(p.unit)}</small></div>
      <div class="product-actions">
        <div class="stepper">
          <button type="button" data-step="-1" aria-label="Less">−</button>
          <input type="number" min="1" max="100" value="1" aria-label="Quantity of ${escapeHtml(p.name)}">
          <button type="button" data-step="1" aria-label="More">+</button>
        </div>
        <button class="btn small" data-add ${p.availableQty <= 0 ? "disabled" : ""}>Add</button>
      </div>
    </article>`).join("");

  grid.querySelectorAll(".product").forEach((card) => {
    const product = products.find((p) => p.id === Number(card.dataset.id));
    const qty = card.querySelector("input");
    card.querySelectorAll("[data-step]").forEach((b) =>
      b.addEventListener("click", () => {
        qty.value = Math.min(100, Math.max(1, Number(qty.value) + Number(b.dataset.step)));
      }));
    card.querySelector("[data-add]").addEventListener("click", () => {
      const n = Math.max(1, Math.min(100, Number.parseInt(qty.value, 10) || 1));
      cart.add(product, n);
      toast(`Added ${n} ${product.unit} ${product.name}`, "ok");
    });
  });
}

api.products().then(render).catch((err) => {
  grid.innerHTML = `<div class="card empty">${escapeHtml(err.message)}<br><span class="hint">Start inventory-service on port 8082.</span></div>`;
});
