# Medicine Finder (Java + Spring Boot)

Search for a medicine and see nearby pharmacies that have it in stock, on a list and a map.

## Run
Requires Java 17+ and Maven.

    mvn spring-boot:run

Open http://localhost:8080

## Make it show your area
Sample pharmacies are generated around the point in `src/main/resources/application.properties`:

    app.seed.center-lat=...
    app.seed.center-lng=...

Set these to your own coordinates and restart. The browser asks for your location; if it is blocked,
the map falls back to this point.

## API
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/search?q=&lat=&lng=&radiusKm=` | Pharmacies with stock, nearest first |
| GET | `/api/medicines/suggest?q=` | Autocomplete names |
| GET | `/api/config` | Default map centre |
| POST | `/api/admin/pharmacies` | Add a pharmacy |
| POST | `/api/admin/stock` | Add or update a stock line |

Example:

    curl -X POST localhost:8080/api/admin/stock -H 'Content-Type: application/json' \
      -d '{"pharmacyId":1,"medicineName":"Paracetamol 500mg","quantity":40,"price":22}'

## Before going live
- Add authentication to `/api/admin/**` (Spring Security).
- Replace H2 with MySQL/PostgreSQL (change `spring.datasource.*`, set `ddl-auto=update`).
- Opening hours use the server's clock; set the server time zone to the pharmacies' zone.
- Medicine availability is information only. Always confirm with the pharmacy, and prescription
  medicines need a valid prescription.
