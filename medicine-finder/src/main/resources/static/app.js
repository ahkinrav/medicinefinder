(() => {
  const form = document.getElementById("search-form");
  const input = document.getElementById("q");
  const radiusSelect = document.getElementById("radius");
  const suggestions = document.getElementById("suggestions");
  const statusEl = document.getElementById("location-status");
  const summary = document.getElementById("summary");
  const list = document.getElementById("results");
  const submitBtn = form.querySelector("button[type=submit]");

  let defaultCenter = { lat: 12.9716, lng: 77.5946 };
  let center = null;          // where the search is anchored
  let markers = [];           // [{ marker, el, item }]
  const map = L.map("map", { zoomControl: true }).setView([defaultCenter.lat, defaultCenter.lng], 13);
  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);
  const markerLayer = L.layerGroup().addTo(map);
  let meMarker = null;

  const pinIcon = (active) => L.divIcon({
    className: "", html: `<div class="pin${active ? " active" : ""}"></div>`,
    iconSize: [26, 26], iconAnchor: [13, 26], popupAnchor: [0, -28]
  });

  function setCenter(lat, lng, label) {
    center = { lat, lng };
    statusEl.textContent = label;
    if (meMarker) meMarker.remove();
    meMarker = L.marker([lat, lng], {
      icon: L.divIcon({ className: "", html: '<div class="me"></div>', iconSize: [16, 16] }),
      keyboard: false
    }).addTo(map);
    map.setView([lat, lng], 14);
  }

  function locate() {
    statusEl.textContent = "Finding your location…";
    if (!navigator.geolocation) {
      setCenter(defaultCenter.lat, defaultCenter.lng, "Location unavailable. Showing the sample area.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => setCenter(pos.coords.latitude, pos.coords.longitude, "Using your current location"),
      () => setCenter(defaultCenter.lat, defaultCenter.lng,
                      "Location blocked. Showing the sample area."),
      { enableHighAccuracy: true, timeout: 8000 }
    );
  }

  async function init() {
    try {
      const res = await fetch("/api/config");
      if (res.ok) {
        const c = await res.json();
        defaultCenter = { lat: c.defaultLat, lng: c.defaultLng };
      }
    } catch { /* keep built-in default */ }
    locate();
  }

  // Autocomplete
  let suggestTimer;
  input.addEventListener("input", () => {
    clearTimeout(suggestTimer);
    const q = input.value.trim();
    if (q.length < 2) return;
    suggestTimer = setTimeout(async () => {
      try {
        const res = await fetch("/api/medicines/suggest?q=" + encodeURIComponent(q));
        const names = await res.json();
        suggestions.replaceChildren(...names.map((n) => {
          const o = document.createElement("option");
          o.value = n;
          return o;
        }));
      } catch { /* suggestions are optional */ }
    }, 200);
  });

  document.getElementById("locate").addEventListener("click", locate);

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!center) { summary.textContent = "Waiting for your location…"; return; }
    await runSearch();
  });

  async function runSearch() {
    const q = input.value.trim();
    const radius = radiusSelect.value;
    submitBtn.disabled = true;
    summary.className = "summary";
    summary.textContent = "Searching…";
    list.replaceChildren();
    clearMarkers();

    try {
      const url = `/api/search?q=${encodeURIComponent(q)}&lat=${center.lat}&lng=${center.lng}&radiusKm=${radius}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error("Search failed. Try again.");
      render(await res.json(), q, radius);
    } catch (err) {
      summary.className = "summary error";
      summary.textContent = err.message || "Could not reach the server.";
    } finally {
      submitBtn.disabled = false;
    }
  }

  function clearMarkers() {
    markerLayer.clearLayers();
    markers = [];
  }

  function render(items, q, radius) {
    if (items.length === 0) {
      summary.textContent = "";
      const p = document.createElement("li");
      p.className = "empty";
      p.textContent = `No pharmacy within ${radius} km has "${q}" in stock. Try a larger radius or a generic name like "paracetamol".`;
      list.append(p);
      return;
    }

    summary.textContent = `${items.length} pharmac${items.length === 1 ? "y" : "ies"} with "${q}" within ${radius} km`;
    const bounds = L.latLngBounds([[center.lat, center.lng]]);

    items.forEach((item) => {
      const li = document.createElement("li");
      const card = buildCard(item);
      li.append(card);
      list.append(li);

      const marker = L.marker([item.latitude, item.longitude], { icon: pinIcon(false) })
        .addTo(markerLayer)
        .bindPopup(popupContent(item));
      marker.on("click", () => select(item.stockId));
      bounds.extend([item.latitude, item.longitude]);
      markers.push({ marker, card, item });

      card.addEventListener("click", (ev) => {
        if (ev.target.closest("a")) return; // let call/directions links work
        select(item.stockId, true);
      });
    });

    map.fitBounds(bounds, { padding: [48, 48], maxZoom: 16 });
  }

  function select(stockId, pan) {
    markers.forEach(({ marker, card, item }) => {
      const active = item.stockId === stockId;
      card.setAttribute("aria-current", active ? "true" : "false");
      marker.setIcon(pinIcon(active));
      if (active) {
        if (pan) map.panTo([item.latitude, item.longitude]);
        marker.openPopup();
        card.scrollIntoView({ block: "nearest" });
      }
    });
  }

  // Everything below builds DOM with textContent so pharmacy data can never inject HTML.
  function el(tag, className, text) {
    const n = document.createElement(tag);
    if (className) n.className = className;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  function buildCard(item) {
    const card = el("div", "result");
    card.tabIndex = 0;
    card.setAttribute("role", "button");
    card.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { e.preventDefault(); card.click(); }
    });

    card.append(el("h2", "", item.pharmacyName));
    card.append(el("p", "addr", item.address || ""));
    card.append(el("p", "med", `${item.medicineName} · ₹${Number(item.price).toFixed(0)}`));

    const capsule = el("span", "capsule");
    capsule.append(el("b", "", `${item.distanceKm.toFixed(1)} km`));
    capsule.append(el("i", item.quantity <= 5 ? "low" : "",
      item.quantity <= 5 ? `Only ${item.quantity} left` : `${item.quantity} in stock`));
    card.append(capsule);
    card.append(el("span", "badge " + (item.openNow ? "open" : "closed"),
      item.openNow ? "Open now" : "Closed now"));

    const actions = el("div", "actions");
    const dir = el("a", "", "Get directions");
    dir.href = `https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}`;
    dir.target = "_blank";
    dir.rel = "noopener";
    actions.append(dir);
    if (item.phone) {
      const call = el("a", "", "Call");
      call.href = "tel:" + item.phone.replace(/[^+\d]/g, "");
      actions.append(call);
    }
    card.append(actions);
    return card;
  }

  function popupContent(item) {
    const box = el("div");
    box.append(el("strong", "", item.pharmacyName));
    box.append(el("br"));
    box.append(document.createTextNode(`${item.hours} · ${item.distanceKm.toFixed(1)} km away`));
    return box;
  }

  init();
})();
