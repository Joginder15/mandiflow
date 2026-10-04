import { api, rupees } from "./api.js";
import { escapeHtml, mountHeader, toast } from "./layout.js";

mountHeader("admin");
const app = document.querySelector("#app");
app.innerHTML = `
  <div class="page-head">
    <h1>Mandi admin</h1>
    <p class="muted">Set today's price and stock. A price change evicts the Redis cache entry for that product.</p>
  </div>
  <div class="admin-grid">
    <section class="card">
      <h2>Produce</h2>
      <div class="table-wrap" id="products"><p class="muted">Loading…</p></div>
      <form class="add-form" id="addForm">
        <h2 class="wide">Add produce</h2>
        <label class="field wide">Name<input name="name" required maxlength="100" placeholder="Bhindi"></label>
        <label class="field">Unit<input name="unit" required maxlength="20" placeholder="kg"></label>
        <label class="field">Price per unit (₹)<input name="pricePerUnit" type="number" min="0.01" step="0.01" required></label>
        <label class="field">Stock<input name="availableQty" type="number" min="0" step="1" required></label>
        <div class="field" style="justify-content:flex-end"><button class="btn" type="submit">Add</button></div>
      </form>
    </section>
    <section class="card">
      <h2>Selling now</h2>
      <p class="hint" id="window">Units sold in the current 5-minute window, from Kafka Streams.</p>
      <ul class="bars" id="bars"></ul>
    </section>
  </div>`;

let names = {};

async function loadProducts() {
  const host = app.querySelector("#products");
  try {
    const products = await api.products();
    names = Object.fromEntries(products.map((p) => [p.id, p.name]));
    host.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>Produce</th><th class="num">Price ₹</th><th class="num">Stock</th><th class="num">Held</th><th></th></tr></thead>
        <tbody>
          ${products.map((p) => `
            <tr data-id="${p.id}">
              <td><strong>${escapeHtml(p.name)}</strong><br><span class="hint">per ${escapeHtml(p.unit)}</span></td>
              <td class="num"><input name="price" type="number" min="0.01" step="0.01" value="${p.pricePerUnit}" aria-label="Price of ${escapeHtml(p.name)}"></td>
              <td class="num"><input name="stock" type="number" min="0" step="1" value="${p.availableQty}" aria-label="Stock of ${escapeHtml(p.name)}"></td>
              <td class="num">${p.reservedQty}</td>
              <td class="num"><button class="btn small" data-save>Save</button></td>
            </tr>`).join("")}
        </tbody>
      </table>`;
    host.querySelectorAll("tr[data-id]").forEach((row) => {
      row.querySelector("[data-save]").addEventListener("click", async () => {
        try {
          const p = await api.updateProduct(Number(row.dataset.id), {
            pricePerUnit: Number(row.querySelector('[name="price"]').value),
            availableQty: Number.parseInt(row.querySelector('[name="stock"]').value, 10),
          });
          toast(`${p.name}: ${rupees(p.pricePerUnit)} per ${p.unit}, ${p.availableQty} in stock`, "ok");
        } catch (err) {
          toast(err.message, "error");
        }
      });
    });
  } catch (err) {
    host.innerHTML = `<div class="empty">${escapeHtml(err.message)}</div>`;
  }
}

app.querySelector("#addForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = new FormData(e.currentTarget);
  try {
    await api.createProduct({
      name: form.get("name"),
      unit: form.get("unit"),
      pricePerUnit: Number(form.get("pricePerUnit")),
      availableQty: Number.parseInt(form.get("availableQty"), 10),
    });
    e.currentTarget.reset();
    toast("Produce added", "ok");
    loadProducts();
  } catch (err) {
    toast(err.message, "error");
  }
});

async function loadTopProducts() {
  const bars = app.querySelector("#bars");
  const label = app.querySelector("#window");
  try {
    const top = await api.topProducts();
    const time = (t) => new Date(t).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
    label.textContent = `Units sold ${time(top.windowStart)}–${time(top.windowEnd)}, from Kafka Streams.`;
    const max = Math.max(1, ...top.products.map((p) => p.units));
    bars.innerHTML = top.products.length === 0
      ? `<li class="hint">Nothing sold in this window yet.</li>`
      : top.products.map((p) => `
          <li class="bar-row">
            <span>${escapeHtml(names[p.productId] ?? `Product ${p.productId}`)}</span>
            <strong>${p.units}</strong>
            <span class="bar"><span style="width:${(p.units / max) * 100}%"></span></span>
          </li>`).join("");
  } catch (err) {
    bars.innerHTML = `<li class="hint">${escapeHtml(err.message)} (analytics-service is optional)</li>`;
  }
}

loadProducts().then(() => {
  loadTopProducts();
  setInterval(loadTopProducts, 5000);
});
