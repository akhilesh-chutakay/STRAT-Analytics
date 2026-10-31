# Strat Analytics Web Companion Dashboard & Live In-App Preview

A complete, responsive web companion application replicating the full functionality, intelligence tools, and aesthetic of the Strat Analytics Android app into an exportable, self-contained web platform with in-app live WebView preview and synchronized JSON data portability.

## User Review & Critical Decisions

> [!IMPORTANT]
> The website will be built as an ultra-polished, self-contained single-page application (HTML5/modern CSS/vanilla ES6+) stored in the Android app's assets (`assets/web/index.html`) with zero external CDN dependency failures, accompanied by an in-app **Web Companion** screen powered by Jetpack Compose & Android `WebView` with full export capabilities.

- **Confirmed Decision 1 (App Scope)**: Replicate *everything* from the Android app: Executive Profile & Target ARR goal tracker, Ventures Pipeline (CRUD & Stage movements), Actionable Roadmap Kanban, Signals Discovery Matrix, Portfolio Analytics & SVG Charts, AI Idea/Deal Evaluator, and JSON Data Export/Import.
- **Confirmed Decision 2 (Integration & Delivery)**: Standalone exportable web application bundled inside the Android app assets, viewable immediately in the emulator through a dedicated "Web Companion" screen, and exportable as a standalone `.html` file.
- **Confirmed Decision 3 (Styling & Theming)**: Mirror Strat Analytics' premium dark luxury aesthetic (deep Obsidian slate `#0B0F17`, glowing emerald `#10B981`, electric indigo `#6366F1`, and crisp typography) with desktop and mobile responsive layouts.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a full-featured web companion dashboard for **Strat Analytics**. Founders, venture architects, and studio operators can view their venture portfolio, track and calibrate their target ARR (with no numeral limits), manage ideas across the venture lifecycle, analyze market opportunity signals, run AI venture assessments, and export/import portfolio data.
- **Target Audience / Persona**: Startup studio founders, angel investors, solo entrepreneurs, and portfolio managers who need an expansive web interface on desktop/tablet while maintaining seamless compatibility with their Android mobile app.
- **Key Value**: Delivers complete cross-platform parity. Users can use the Android app on the go, preview the live web version directly inside the app, and export or host the web application anywhere with zero build tools or servers required.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Overview & Executive Dashboard**:
   - High-impact header displaying founder avatar, name, title, bio, dynamic Target ARR progress bar, and Target ARR editor modal.
   - Core KPI tiles: Total Ventures, Active Pipeline ARR, Average Viability Score, and Scaling Ventures count.
   - Quick action bar: "New Venture", "AI Evaluator", "Signals Matrix", "Export Web Bundle", and "Sync Data".
2. **Venture Idea Lifecycle Management (Full CRUD)**:
   - Filter bar with search query, category pills (B2B SaaS, AI/ML, FinTech, HealthTech, ClimateTech, Consumer), and stage tabs (All, Ideation, Validation, Prototype, Launch, Growth, Scaling).
   - Card grid with stage badges, ARR metrics, viability meters, and quick actions.
   - Interactive Modal to add/edit ideas (Problem, Solution, Target Audience, Market Size, Pricing Model, Projected ARR, Viability Score, Milestones).
   - Interactive Detail View with milestone completion toggles and financial breakdowns.
3. **Actionable Roadmap & Kanban Board**:
   - Visual Kanban columns representing venture maturity stages.
   - Stage progression controls (Move Forward / Move Backward) with instant metric updates.
4. **Signals Discovery Matrix**:
   - Curated high-conviction market signals with opportunity ratings, industry tags, and pain-point summaries.
   - "Convert Signal to Venture Idea" button that pre-populates a new venture form with one click.
5. **Portfolio Analytics & Financial Visualizations**:
   - Interactive SVG charts: Stage Funnel breakdown, ARR Distribution by Sector, and Viability vs. Execution Risk scatter plot.
6. **Data Portability & Standalone Export**:
   - Seamless JSON Export/Import fully compatible with the Android app's Room backup schema.
   - "Download Standalone HTML" feature that allows users to save and run the web app locally on any browser or upload to static hosting (GitHub Pages, Vercel, Firebase Hosting, Netlify).

### Visual Identity & Theme
- **Color Palette**:
  - Background Base: `#0B0F17` (Deep Obsidian Void)
  - Surface Primary: `#111827` / `#161F30` (Layered Slate)
  - Primary Accent: `#10B981` (Venture Emerald) & `#34D399`
  - Secondary Accent: `#6366F1` (Studio Indigo) & `#818CF8`
  - Warning/Alerts: `#F59E0B` (Amber) & `#EF4444` (Coral Red)
  - Text: `#F9FAFB` (High contrast primary), `#94A3B8` (Muted secondary)
- **Typography & Atmosphere**:
  - System font stack prioritizing `Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`.
  - Glassmorphic card borders (`border: 1px solid rgba(255, 255, 255, 0.08)`), subtle glow drop-shadows on active metrics, and responsive desktop-first layout with smooth mobile breakpoints.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Architecture of the Web App (Single-File Self-Contained Bundle)**
  - *Chosen Approach*: Build a modular, complete single-file web application (`assets/web/index.html`) containing modern CSS, responsive layout, inline SVG iconography, and modular JavaScript with `localStorage` persistence and JSON import/export.
  - *Why*: Eliminates CORS issues, avoids external asset loading failures inside Android `WebView`, works 100% offline, and allows immediate one-click downloading as an independent website.
  - *Alternatives Considered*: Multi-file build system requiring Webpack/Vite; rejected because it introduces fragile compilation steps and hinders direct offline execution inside the Android WebView container.

- **Decision 2: In-App Integration via Jetpack Compose WebView Screen**
  - *Chosen Approach*: Add a "Web Companion" navigation destination in the Android app featuring a Jetpack Compose `AndroidView` wrapping a configured Android `WebView`, along with top-bar controls for Desktop/Mobile view toggle, Refresh, and Export HTML action.
  - *Why*: Gives users immediate access to test and interact with the website directly within the Android emulator, proving full visual and functional parity.

- **Decision 3: Complete State Parity & Seed Data**
  - *Chosen Approach*: Pre-seed the web application with the same rich venture studio portfolio (AI Deal Evaluator, FinTech API, Green Logistics, HealthOps, etc.) and Founder profile, while enabling instant JSON export/import between the Android app and the web app.
  - *Why*: Ensures the website is immediately rich, lively, and fully usable out of the box.

---

## 4. Technical Architecture & Data Strategy

### System & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        STRAT ANALYTICS APPLET                          │
├─────────────────────────────────────┬──────────────────────────────────┤
│         ANDROID APP LAYER           │         WEB COMPANION LAYER      │
│  (Kotlin / Jetpack Compose / M3)    │    (HTML5 / CSS3 / ES6 Engine)   │
├─────────────────────────────────────┼──────────────────────────────────┤
│ • HomeScreen & Navigation           │ • assets/web/index.html          │
│ • WebCompanionScreen (Compose)      │ • Modular Vanilla JS State Store │
│   └─ AndroidView(WebView)           │ • Responsive Glassmorphism UI    │
│   └─ Desktop/Mobile Viewport Toggle │ • Interactive SVG Chart Engine   │
│   └─ Export / Share Web Page        │ • LocalStorage & JSON Backup     │
│ • BottomBar & Drawer Link           │ • AI Venture Deal Evaluator      │
├─────────────────────────────────────┴──────────────────────────────────┤
│                           DATA PARITY ENGINE                           │
│  • VentureIdea Model & JSON Schema Match                               │
│  • Profile & Target ARR Parity ($1M -> $100B+)                         │
│  • Signals Matrix & Actionable Roadmap Sync                            │
└────────────────────────────────────────────────────────────────────────┘
```

### Key Modules in `assets/web/index.html`
1. **State Store (`StratStore`)**:
   - Manages `ventures`, `profile`, `signals`, `filterState`, `activeTab`.
   - Automatically synchronizes with `window.localStorage`.
   - Handles JSON serialization matching the Android app's `VentureIdea` model.
2. **UI View Renderers**:
   - `renderDashboard()`: Metric cards, Target ARR progress ring/bar, ARR milestones.
   - `renderVentures()`: Filterable grid/list with stage tags, viability bars, and action menus.
   - `renderKanban()`: Multi-stage columns with stage-advance buttons.
   - `renderSignals()`: Trending opportunity cards with quick conversion to ventures.
   - `renderAnalytics()`: SVG Funnel chart, ARR distribution by domain, and Risk/Viability matrix.
   - `renderAIEvaluator()`: Interactive form computing viability scores, risk assessments, and ARR projections.
3. **Interactive Modals**:
   - Idea Creation / Edit Modal with form validation.
   - Venture Detail Drawer / Modal with roadmap milestone toggles.
   - Target ARR Goal Editor with quick presets ($1M, $10M, $100M, $1B, etc.).
   - Data Backup / Export / Import Modal.

### Android Integration (`WebCompanionScreen.kt`)
- Created in `com.example.ui.screens.WebCompanionScreen.kt`.
- Uses Compose `AndroidView` with `WebViewClient`, WebChromeClient, and JavaScript enabled.
- Loads `file:///android_asset/web/index.html`.
- Includes top app bar actions:
  - Toggle Viewport Mode (Desktop Wide vs Mobile Emulation).
  - Open in External System Browser via `Intent.ACTION_VIEW`.
  - Share / Export HTML file.
  - Quick refresh.
- Navigation link added to the main app navigation and top app bar for instant 1-tap switching.
