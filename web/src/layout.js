import { cart, customer } from "./store.js";

// Shared header: brand, navigation, cart count and the demo customer switcher.
export function mountHeader(active) {
  const header = document.createElement("header");
  header.className = "site-header";
  header.innerHTML = `
    <a class="brand" href="/index.html" aria-label="MandiFlow home">
      <span class="brand-dots" aria-hidden="true"><i></i><i></i><i></i></span>
      <span>Mandi<b>Flow</b></span>
    </a>
    <nav class="nav" aria-label="Main">
      <a href="/index.html" data-page="catalogue">Catalogue</a>
      <a href="/cart.html" data-page="cart">Cart <span class="badge" id="cartCount">0</span></a>
      <a href="/track.html" data-page="track">My orders</a>
      <a href="/admin.html" data-page="admin">Admin</a>
    </nav>
    <label class="customer">
      <span>Customer</span>
      <input id="customerId" type="number" min="1" inputmode="numeric" aria-label="Customer ID">
    </label>`;
  document.body.prepend(header);

  header.querySelector(`[data-page="${active}"]`)?.setAttribute("aria-current", "page");

  const input = header.querySelector("#customerId");
  input.value = customer.id();
  input.addEventListener("change", () => {
    const id = Number.parseInt(input.value, 10);
    if (id > 0) customer.set(id);
    else input.value = customer.id();
  });

  const count = header.querySelector("#cartCount");
  const refresh = () => {
    const n = cart.count();
    count.textContent = n;
    count.hidden = n === 0;
  };
  refresh();
  window.addEventListener("mandiflow:change", refresh);
  window.addEventListener("storage", refresh);
}

export function toast(message, kind = "info") {
  let host = document.querySelector(".toasts");
  if (!host) {
    host = document.createElement("div");
    host.className = "toasts";
    host.setAttribute("role", "status");
    document.body.append(host);
  }
  const el = document.createElement("div");
  el.className = `toast ${kind}`;
  el.textContent = message;
  host.append(el);
  setTimeout(() => el.remove(), 4000);
}

/** A coloured tile with the product's initial, so the catalogue needs no images. */
export function produceTile(name) {
  const hues = [12, 38, 95, 140, 175, 205, 260, 330];
  let h = 0;
  for (const ch of name) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  const hue = hues[h % hues.length];
  return `<span class="tile" style="--h:${hue}" aria-hidden="true">${name.trim()[0] ?? "?"}</span>`;
}

export const escapeHtml = (s) =>
  String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
