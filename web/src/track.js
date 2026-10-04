import { api, rupees } from "./api.js";
import { escapeHtml, mountHeader } from "./layout.js";
import { customer } from "./store.js";

mountHeader("track");
const app = document.querySelector("#app");
const orderId = new URLSearchParams(location.search).get("id");

const STEPS = ["Order placed", "Stock held", "Payment", "Confirmed"];
// For each status: how many steps are done, and which step (if any) failed.
const PROGRESS = {
  PENDING: { done: 1, failed: -1 },
  REJECTED: { done: 1, failed: 1 },
  CANCELLED: { done: 2, failed: 2 },
  CONFIRMED: { done: 4, failed: -1 },
};
const LABEL = {
  PENDING: "Waiting for inventory and payment",
  CONFIRMED: "Confirmed",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
};

const shortId = (id) => id.slice(0, 8).toUpperCase();

if (orderId) showOrder(orderId);
else showRecent();

async function showOrder(id) {
  app.innerHTML = `<p class="muted">Loading order…</p>`;
  let order;
  let names = {};
  try {
    [order, names] = await Promise.all([
      api.order(id),
      api.products()
        .then((ps) => Object.fromEntries(ps.map((p) => [p.id, `${p.name} (${p.unit})`])))
        .catch(() => ({})),
    ]);
  } catch (err) {
    app.innerHTML = `<div class="card empty">${escapeHtml(err.message)}</div>`;
    return;
  }

  app.innerHTML = `
    <div class="page-head">
      <h1>Order ${shortId(order.id)}</h1>
      <p class="muted">Placed ${new Date(order.createdAt).toLocaleString("en-IN")} · <span class="mono">${order.id}</span></p>
    </div>
    <section class="card status-card" aria-live="polite">
      <span class="pill" id="status"></span>
      <ol class="steps" id="steps"></ol>
      <div class="reason" id="reason" hidden></div>
      <p class="hint" id="live"></p>
    </section>
    <section class="card table-wrap">
      <table>
        <thead><tr><th>Item</th><th class="num">Qty</th><th class="num">Price</th><th class="num">Amount</th></tr></thead>
        <tbody>
          ${order.items.map((i) => `
            <tr>
              <td>${escapeHtml(names[i.productId] ?? `Product ${i.productId}`)}</td>
              <td class="num">${i.quantity}</td>
              <td class="num">${rupees(i.unitPrice)}</td>
              <td class="num">${rupees(i.lineTotal)}</td>
            </tr>`).join("")}
          <tr><td colspan="3"><strong>Total</strong></td><td class="num"><strong>${rupees(order.totalAmount)}</strong></td></tr>
        </tbody>
      </table>
    </section>`;

  showStatus(order.status, order.statusReason);
  if (order.status === "PENDING") listen(id);
}

function showStatus(status, reason) {
  const pill = app.querySelector("#status");
  pill.textContent = LABEL[status] ?? status;
  pill.dataset.state = status.toLowerCase();

  const { done, failed } = PROGRESS[status] ?? PROGRESS.PENDING;
  app.querySelector("#steps").innerHTML = STEPS.map((s, i) => {
    const cls = i === failed ? "failed" : i < done ? "done" : "";
    return `<li class="${cls}">${s}</li>`;
  }).join("");

  const box = app.querySelector("#reason");
  box.hidden = !reason;
  box.textContent = reason ?? "";
}

// Server-Sent Events: the status changes without a refresh.
function listen(id) {
  const live = app.querySelector("#live");
  live.textContent = "Live: this page updates on its own.";
  const source = api.orderEvents(id);
  source.onmessage = (msg) => {
    const update = JSON.parse(msg.data);
    showStatus(update.status, update.reason);
    if (update.status !== "PENDING") {
      source.close();
      live.textContent = "Done. An email is on its way to Mailpit (localhost:8025).";
    }
  };
  source.onerror = () => {
    if (source.readyState === EventSource.CLOSED) live.textContent = "Live updates stopped. Refresh to check again.";
  };
}

async function showRecent() {
  const id = customer.id();
  app.innerHTML = `
    <div class="page-head">
      <h1>My orders</h1>
      <p class="muted">The last 20 orders of customer #${id}.</p>
    </div>
    <div class="card table-wrap" id="list"><p class="muted">Loading…</p></div>`;
  const list = app.querySelector("#list");
  try {
    const orders = await api.recentOrders(id);
    if (orders.length === 0) {
      list.innerHTML = `<div class="empty">No orders yet. <a href="/index.html">Go shopping</a>.</div>`;
      return;
    }
    list.innerHTML = `
      <table>
        <thead><tr><th>Order</th><th>Placed</th><th>Status</th><th class="num">Total</th></tr></thead>
        <tbody>
          ${orders.map((o) => `
            <tr>
              <td><a class="mono" href="/track.html?id=${o.id}">${shortId(o.id)}</a></td>
              <td>${new Date(o.createdAt).toLocaleString("en-IN")}</td>
              <td><span class="pill" data-state="${o.status.toLowerCase()}">${o.status}</span></td>
              <td class="num">${rupees(o.totalAmount)}</td>
            </tr>`).join("")}
        </tbody>
      </table>`;
  } catch (err) {
    list.innerHTML = `<div class="empty">${escapeHtml(err.message)}</div>`;
  }
}

window.addEventListener("mandiflow:change", () => { if (!orderId) showRecent(); });
