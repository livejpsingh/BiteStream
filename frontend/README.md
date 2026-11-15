# BiteStream Angular Web Dashboard (`frontend/`)

Modern, high-performance web console for the **BiteStream Real-Time Event-Driven Food Delivery & Dispatch Platform** built with **Angular (v22)** and **Tailwind CSS**.

---

## 🌟 Features Implemented

1. **Active Restaurant Catalog (`RestaurantCatalogComponent`)**:
   - Lists 5 active restaurants with cuisine types, operating hours, coordinates, and contact details.
   - Live dish card viewer for 10 menu items per store with prep times, pricing, and "+ Add" cart actions.
2. **Dynamic Logistics & Order Builder (`OrderPlacementComponent`)**:
   - Distance selector band (**2.0 km, 3.0 km, 4.0 km, 5.0 km**).
   - Real-time travel time calculation based on city transit formula:
     $$\text{Transit Time} = \left(\frac{\text{Distance}}{20\text{ km/h}} \times 60\right) + 4\text{ min buffer}$$
   - Dynamic total ETA calculation incorporating kitchen queue delay:
     $$\text{Total ETA} = \text{Max Dish Prep Time} + (\text{Queue Position} \times 3\text{m}) + \text{Transit Time}$$
   - Direct Kafka event dispatch (`food.orders.placed`).
3. **Live Dispatch & Event Mesh Tracker (`LiveDispatchTrackerComponent`)**:
   - Live Order State Machine: `ORDER_PLACED` &rarr; `KITCHEN_PREPARING` &rarr; `PICKED_UP` &rarr; `DELIVERED`.
   - Two-Step OTP Protocol Interactive Verification Cards:
     - **Step 1: Restaurant Pickup Handover** (15-min Redis TTL).
     - **Step 2: Customer Doorstep Handover** (30-min Redis TTL).
   - **Delivery Fleet Grid**: Status of 20 active riders (Electric Scooter, Honda Activa, Ather 450X, Yamaha FZ) with ratings and completion counters.
   - **Kafka Stream Monitor**: Live topic listeners and consumer group assignment viewer.
4. **Interactive 2-Step OTP Verification Modal (`OtpModalComponent`)**:
   - Auto-fill helper displaying active OTP stored in Redis.
   - Real API verification calls to `POST /api/deliveries/{orderId}/pickup` and `POST /api/deliveries/{orderId}/complete`.

---

## 📐 Architecture & CSS Isolation Standard

To ensure enterprise-level modularity where new features can be developed without impacting existing components, this project strictly adheres to **Angular ViewEncapsulation**:

```
frontend/
├── src/
│   ├── styles.css                     # Common / Global Styles (Tailwind base, design tokens, resets)
│   ├── app/
│   │   ├── core/                      # Singleton Services, Models, API Integration
│   │   │   ├── models/                # TypeScript Interfaces (Restaurant, Order, DeliveryAgent)
│   │   │   └── services/              # ApiService with live backend proxy & resilient offline fallback
│   │   ├── shared/                    # Shared Reusable Components
│   │   │   └── components/
│   │   │       ├── navbar/            # Navbar (Isolated: navbar.component.css)
│   │   │       ├── stats-bar/         # Stats Bar (Isolated: stats-bar.component.css)
│   │   │       └── otp-modal/         # OTP Modal (Isolated: otp-modal.component.css)
│   │   ├── features/                  # Feature Components (Strictly Isolated Styles)
│   │   │   ├── restaurant-catalog/    # Catalog (Isolated: restaurant-catalog.component.css)
│   │   │   ├── order-placement/       # Order Builder (Isolated: order-placement.component.css)
│   │   │   └── live-dispatch-tracker/ # Event Tracker (Isolated: live-dispatch-tracker.component.css)
│   │   ├── app.ts                     # Root Component
│   │   ├── app.html                   # Master Dashboard Layout
│   │   └── app.css                    # Root Layout Styles
│   ├── proxy.conf.json                # Dev proxy routing /api & /actuator to localhost:8080
│   └── tailwind.config.js             # Tailwind CSS theme & color configuration
```

### CSS Guidelines:
- **Global Styles (`src/styles.css`)**: Only design system variables (`--color-brand`), base utility classes (`.glass-card`, `.btn-brand`, `.badge-tag`), and scrollbar styling.
- **Component Styles (`*.component.css`)**: Component-specific scoped styling using Angular's host-element encapsulation. No style rules can leak across feature boundaries.

---

## 🚀 Running the Frontend

### 1. Install Dependencies
```bash
cd frontend
npm install
```

### 2. Start Development Server
```bash
npm start
# Or
ng serve
```
The app will be available at [http://localhost:4200](http://localhost:4200). All API requests to `/api/*` and `/actuator/*` are automatically proxied to the BiteStream Spring Boot backend on port `8080`.

### 3. Production Build
```bash
npm run build
```
Production assets are generated in `dist/frontend`.
