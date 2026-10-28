(() => {
  const form = document.getElementById("search-form");
  const input = document.getElementById("q");
  const radiusSelect = document.getElementById("radius");
  const suggestions = document.getElementById("suggestions");
  const statusEl = document.getElementById("location-status");
  const summary = document.getElementById("summary");
  const list = document.getElementById("results");
  const submitBtn = form.querySelector('button[type="submit"]');

  let defaultCenter = { lat: 10.9175, lng: 78.7320 };
  let center = null;
  let markers = [];

  const map = L.map("map", { zoomControl: true }).setView(
    [defaultCenter.lat, defaultCenter.lng],
    13
  );

  L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
  }).addTo(map);

  const markerLayer = L.layerGroup().addTo(map);
  let meMarker = null;

  const pinIcon = (active) =>
    L.divIcon({
      className: "",
      html: `<div class="pin${active ? " active" : ""}"></div>`,
      iconSize: [26, 26],
      iconAnchor: [13, 26],
      popupAnchor: [0, -28]
    });

  function setCenter(lat, lng, label) {
    center = { lat, lng };
    statusEl.textContent = label;

    if (meMarker) {
      meMarker.remove();
    }

    meMarker = L.marker([lat, lng], {
      icon: L.divIcon({
        className: "",
        html: '<div class="me"></div>',
        iconSize: [16, 16],
        iconAnchor: [8, 8]
      }),
      keyboard: false
    }).addTo(map);

    map.setView([lat, lng], 14);
  }

  function locate() {
    statusEl.textContent = "Finding your location...";

    if (!navigator.geolocation) {
      setCenter(
        defaultCenter.lat,
        defaultCenter.lng,
        "Location unavailable. Showing the default area."
      );
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCenter(
          pos.coords.latitude,
          pos.coords.longitude,
          "Using your current location"
        );
      },
      () => {
        setCenter(
          defaultCenter.lat,
          defaultCenter.lng,
          "Location unavailable. Showing the default area."
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 8000,
        maximumAge: 60000
      }
    );
  }

  async function init() {
    try {
      const res = await fetch("/api/config");

      if (res.ok) {
        const config = await res.json();

        if (
          Number.isFinite(Number(config.defaultLat)) &&
          Number.isFinite(Number(config.defaultLng))
        ) {
          defaultCenter = {
            lat: Number(config.defaultLat),
            lng: Number(config.defaultLng)
          };
        }
      }
    } catch {
      // Use the default coordinates above.
    }

    locate();
  }

  let suggestTimer;

  input.addEventListener("input", () => {
    clearTimeout(suggestTimer);

    const q = input.value.trim();

    if (q.length < 2) {
      suggestions.replaceChildren();
      return;
    }

    suggestTimer = setTimeout(async () => {
      try {
        const res = await fetch(
          "/api/medicines/suggest?q=" + encodeURIComponent(q)
        );

        if (!res.ok) return;

        const names = await res.json();

        suggestions.replaceChildren(
          ...names.map((name) => {
            const option = document.createElement("option");
            option.value = name;
            return option;
          })
        );
      } catch {
        // Suggestions are optional.
      }
    }, 250);
  });

  document.getElementById("locate").addEventListener("click", locate);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (!input.value.trim()) {
      summary.textContent = "Please enter a medicine name.";
      input.focus();
      return;
    }

    if (!center) {
      summary.textContent = "Waiting for your location...";
      return;
    }

    await runSearch();
  });

  async function runSearch() {
    const q = input.value.trim();
    const radius = radiusSelect.value;

    submitBtn.disabled = true;
    summary.className = "summary";
    summary.textContent = "Searching for nearby pharmacies...";
    list.replaceChildren();
    clearMarkers();

    try {
      const url =
        `/api/search?q=${encodeURIComponent(q)}` +
        `&lat=${center.lat}` +
        `&lng=${center.lng}` +
        `&radiusKm=${encodeURIComponent(radius)}`;

      const response = await fetch(url);

      if (!response.ok) {
        throw new Error("Search failed. Please try again.");
      }

      const items = await response.json();

      if (!Array.isArray(items)) {
        throw new Error("Unexpected response from the server.");
      }

      render(items, q, radius);
    } catch (error) {
      summary.className = "summary error";
      summary.textContent =
        error.message || "Could not connect to the server.";
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

      const empty = document.createElement("li");
      empty.className = "empty";
      empty.textContent =
        `No pharmacy within ${radius} km has "${q}" in stock. ` +
        'Try a larger radius or a generic medicine name such as "paracetamol".';

      list.append(empty);
      return;
    }

    summary.textContent =
      `${items.length} ${items.length === 1 ? "pharmacy" : "pharmacies"} ` +
      `found for "${q}" within ${radius} km.`;

    const bounds = L.latLngBounds([[center.lat, center.lng]]);

    items.forEach((item) => {
      const latitude = Number(item.latitude);
      const longitude = Number(item.longitude);

      const li = document.createElement("li");
      const card = buildCard(item);

      li.append(card);
      list.append(li);

      if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
        const marker = L.marker([latitude, longitude], {
          icon: pinIcon(false)
        })
          .addTo(markerLayer)
          .bindPopup(popupContent(item));

        marker.on("click", () => select(item.stockId));

        bounds.extend([latitude, longitude]);

        markers.push({
          marker,
          card,
          item
        });
      }

      card.addEventListener("click", (event) => {
        if (event.target.closest("a")) return;
        select(item.stockId, true);
      });
    });

    if (markers.length > 0) {
      map.fitBounds(bounds, {
        padding: [40, 40],
        maxZoom: 16
      });
    }
  }

  function select(stockId, pan = false) {
    markers.forEach(({ marker, card, item }) => {
      const active = item.stockId === stockId;

      card.setAttribute("aria-current", active ? "true" : "false");
      marker.setIcon(pinIcon(active));

      if (active) {
        if (pan) {
          map.panTo([item.latitude, item.longitude]);
        }

        marker.openPopup();
        card.scrollIntoView({
          block: "nearest",
          behavior: "smooth"
        });
      }
    });
  }

  function el(tag, className, text) {
    const element = document.createElement(tag);

    if (className) {
      element.className = className;
    }

    if (text !== undefined && text !== null) {
      element.textContent = String(text);
    }

    return element;
  }

  function buildCard(item) {
    const card = el("div", "result");
    card.tabIndex = 0;
    card.setAttribute("role", "button");

    card.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        card.click();
      }
    });

    card.append(el("h2", "", item.pharmacyName || "Pharmacy"));
    card.append(el("p", "addr", item.address || "Address unavailable"));

    const price = Number(item.price);
    const priceText = Number.isFinite(price)
      ? `₹${price.toFixed(2)}`
      : "Price unavailable";

    card.append(
      el(
        "p",
        "med",
        `${item.medicineName || "Medicine"} · ${priceText}`
      )
    );

    const capsule = el("span", "capsule");

    const distance = Number(item.distanceKm);
    capsule.append(
      el(
        "b",
        "",
        Number.isFinite(distance) ? `${distance.toFixed(1)} km` : "Nearby"
      )
    );

    const quantity = Number(item.quantity);
    const quantityText = Number.isFinite(quantity)
      ? quantity <= 5
        ? `Only ${quantity} left`
        : `${quantity} in stock`
      : "Stock information unavailable";

    capsule.append(
      el("i", Number.isFinite(quantity) && quantity <= 5 ? "low" : "", quantityText)
    );

    card.append(capsule);

    card.append(
      el(
        "span",
        "badge " + (item.openNow ? "open" : "closed"),
        item.openNow ? "Open now" : "Closed now"
      )
    );

    const actions = el("div", "actions");

    const latitude = Number(item.latitude);
    const longitude = Number(item.longitude);

    if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
      const directions = el("a", "", "Get directions");

      directions.href =
        "https://www.google.com/maps/dir/?api=1&destination=" +
        `${latitude},${longitude}`;

      directions.target = "_blank";
      directions.rel = "noopener noreferrer";

      actions.append(directions);
    }

    if (item.phone) {
      const call = el("a", "", "Call");
      call.href = "tel:" + String(item.phone).replace(/[^\d+]/g, "");
      actions.append(call);
    }

    card.append(actions);

    return card;
  }

  function popupContent(item) {
    const box = el("div");
    box.append(el("strong", "", item.pharmacyName || "Pharmacy"));
    box.append(el("br"));

    const distance = Number(item.distanceKm);
    const distanceText = Number.isFinite(distance)
      ? `${distance.toFixed(1)} km away`
      : "Distance unavailable";

    box.append(
      document.createTextNode(
        `${item.hours || "Hours unavailable"} · ${distanceText}`
      )
    );

    return box;
  }

  init();
})();