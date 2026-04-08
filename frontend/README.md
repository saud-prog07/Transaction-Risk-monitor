# Frontend - Transaction Risk Monitoring Dashboard

Professional React.js dashboard for real-time fraud detection, risk analysis, and alert management.

## Overview

A modern, fully-featured web dashboard showcasing:
- Real-time Alerts: Live transaction feed with 3-second polling intervals
- Advanced Analytics: Risk metrics, statistical data visualization
- Professional UI: Responsive design with color-coded risk levels
- Alert Management: Multi-status filtering (NEW, REVIEWED, ESCALATED)
- Transaction Simulator: Integrated testing tool (Normal & Fraud modes)
- Health Monitoring: Real-time backend API status

Built with:
- React 18 - Component-based UI with hooks
- Axios - HTTP client with interceptors
- CSS3 - Custom styling (no Bootstrap/Material - pure CSS for learning)
- Custom Hooks - usePolling for real-time data, custom state management

## Advanced Component Architecture

```
frontend/
└── dashboard/                               # React SPA
    └── src/
        ├── components/
        │   ├── OperationsDashboard.js           # Orchestrator (state + polling)
        │   ├── LiveTransactionsFeed.js         # Real-time transaction stream
        │   ├── AlertsPanel.js                 # Alert list with filtering
        │   ├── SystemHealth.js                # Backend service status
        │   ├── TransactionSimulator.js         # Fraud pattern generator (NEW!)
        │   ├── RiskBadge.js                   # Color-coded risk levels
        │   ├── StatCard.js                    # Metric dashboard cards
        │   ├── FilterBar.js                   # Advanced search/filtering
        │   ├── GuidedDemoFlow.js              # Interactive demo mode
        │   └── RiskAnalyzerConfigPanel.js     # Risk analyzer tuning UI
        ├── services/
        │   ├── apiService.js                  # Centralized API client
        │   └── utils.js                       # Formatting & helpers
        ├── hooks/
        │   └── usePolling.js                  # Real-time data polling
        ├── styles/
        │   ├── OperationsDashboard.css         # Main layout (CSS Grid)
        │   ├── TransactionSimulator.css        # Simulator dark theme
        │   ├── RiskBadge.css                  # Color scheme (RED/AMBER/BLUE/GREEN)
        │   ├── AlertsPanel.css                # Table styling
        │   └── index.css                      # Global styles
        ├── App.js                         # Router & error boundary
        └── index.js                        # React mount point
```

## Quick Start

### Prerequisites

```bash
node --version              # v14+
npm --version              # 6+
```

Ensure backend Alert Service is running:
```bash
curl http://localhost:8082/api/actuator/health
```

### Installation

```bash
cd frontend/dashboard

# Install dependencies
npm install

# Configure API URL
cp .env.example .env
# Edit .env: REACT_APP_API_URL=http://localhost:8082

# Start development server
npm start
```

Open: `http://localhost:3000`

## � Component Features

### OperationsDashboard (Orchestrator)
- **Real-time Polling** — usePolling hook with 3-second intervals
- **State Management** — React hooks for alerts, filters, pagination
- **Error Handling** — Graceful fallbacks, user-friendly error messages
- **Responsive Grid** — CSS Grid layout for mobile/tablet/desktop

### LiveTransactionsFeed
- **Auto-scrolling** — Last 10 transactions with real-time updates
- **Color Coding** — RED (#EF4444) for HIGH, AMBER (#F59E0B) for MEDIUM, BLUE (#3B82F6) for LOW
- **Expandable** — Click to view full transaction details
- **Status Badges** — NEW, REVIEWED, ESCALATED indicators

### AlertsPanel
- **Advanced Filtering** — By status (NEW/REVIEWED/ESCALATED), risk level (HIGH/MEDIUM/LOW)
- **Sortable Columns** — Amount, risk score, timestamp
- **Pagination** — Page size selector (10, 20, 50 per page)
- **Bulk Actions** — Multi-select, mark as reviewed, escalate

### TransactionSimulator (Phase 8)
- **Normal Mode** — Realistic transactions ($10-$5K, 1-2/sec) for baseline testing
- **Fraud Mode** — Suspicious patterns ($5K-$50K, 5-10/sec, limited merchants)
- **Send Methods**:
  - "Send One" — Immediate single transaction
  - "Send Batch" — Multiple with 200ms delay
  - "Stream" — Continuous generation (stop button available)
- **Statistics** — Real-time counters (total sent, successful, failed, success %)
- **Last 10 Display** — Full transaction details with fraud flags
- **Status Indicators** — Ready, sending, streaming, complete, error states with animations

### SystemHealth
- **Service Status** — Producer, Risk Engine, Alert Service indicators
- **Color Indicators** — GREEN (UP), RED (DOWN), YELLOW (DEGRADED)
- **Response Times** — API latency display
- **Auto-refresh** — Updates every 5 seconds

## 🎨 UI Design System

**Color Scheme** (Risk Levels):
- 🔴 **RED** (#EF4444) — HIGH RISK - Fraud detected
- 🟠 **AMBER** (#F59E0B) — MEDIUM RISK - Suspicious
- 🔵 **BLUE** (#3B82F6) — LOW RISK - Legitimate
- 🟢 **GREEN** (#10B981) — Healthy/Operational

**Styling Approach:**
- Pure CSS3 (no CSS framework)
- Dark theme on OperationsDashboard
- Midnight blue gradient background (#0f172a)
- Responsive breakpoints: Mobile (<768px), Tablet (768-1024px), Desktop (>1024px)
- Smooth animations: Pulse effects, hover transitions, fade-in load states

## API Integration

**apiService.js Methods:**
```javascript
// Alert operations
apiService.getAlerts(page, size, filters)  // Fetch paginated alerts
apiService.updateAlertStatus(id, status)   // Update individual alert
apiService.searchAlerts(query)             // Full-text search
apiService.getAlertById(id)                // Single alert details

// Transaction simulator
apiService.submitTransaction(txData)       // Single transaction
apiService.submitBatchTransactions(txns)   // Batch submission

// Health checks
apiService.checkHealth()                   // Verify backend connectivity
```
- Status management

✅ **Filtering & Search**
- Search by User ID
- Search by Transaction ID
- Filter by Risk Level
- Filter by Status
- Real-time filtering

✅ **UI Components**
- Color-coded risk badges
- Status indicators
- Loading spinners
- Error handling
- Empty states

✅ **Responsive Design**
- Desktop (1024px+)
- Tablet (768px-1023px)
- Mobile (<768px)
- Horizontal scroll for tables

✅ **API Health**
- Real-time connection monitoring
- Health check indicator
- Auto-refresh on health recovery

## 📡 API Integration

The dashboard consumes these Alert Service endpoints:

**Get Alerts**
```bash
GET /api/alerts?page=0&size=20&riskLevel=HIGH&status=NEW
```

**Update Alert Status**
```bash
PUT /api/alerts/{alertId}/status
Content-Type: application/json

{ "status": "REVIEWED" }
```

**Health Check**
```bash
GET /api/actuator/health
```

See [backend/README.md](../backend/README.md) for full API documentation.

## ⚙️ Configuration

### Environment Variables

Create `.env` file:

```bash
# Backend API URL
REACT_APP_API_URL=http://localhost:8082

# App configuration
REACT_APP_APP_NAME=Transaction Risk Monitor
REACT_APP_REFRESH_INTERVAL=30000

# Feature flags
REACT_APP_ENABLE_REAL_TIME=true
```

See `.env.example` for all available variables.

## 🎯 Component Overview

### Dashboard
Main container managing state, data fetching, filtering.

```jsx
<Dashboard />
```

### StatCard
Display statistics with icons and colors.

```jsx
<StatCard
  title="High Risk"
  value={45}
  icon="🔴"
  color="danger"
  subtitle="Critical alerts"
/>
```

### AlertsTable
Render alerts in responsive table.

```jsx
<AlertsTable
  alerts={alerts}
  loading={loading}
  error={error}
  onRefresh={loadAlerts}
  onStatusUpdate={updateAlert}
/>
```

### FilterBar
Search and filter controls.

```jsx
<FilterBar
  onFilterChange={handleFilter}
  onSearchChange={handleSearch}
/>
```

### RiskBadge
Color-coded risk indicator.

```jsx
<RiskBadge level="HIGH" score={87.5} />
```

### StatusBadge
Alert status indicator.

```jsx
<StatusBadge status="NEW" />
```

## 📊 Data Flow

```
Component Mount
    ↓
Check API Health
    ↓
Fetch Alerts (with filters)
    ↓
Calculate Statistics
    ↓
Render Dashboard
    ↓
User Interaction (Search, Filter, Status Update)
    ↓
Update Local State
    ↓
API Call (if needed)
    ↓
Refresh Data
```

## Styling

### Color Scheme

```css
:root {
  --color-primary: #2c3e50;      /* Main blue */
  --color-secondary: #3498db;    /* Light blue */
  --color-success: #27ae60;      /* Green */
  --color-warning: #f39c12;      /* Orange */
  --color-danger: #e74c3c;       /* Red */
  --color-light: #ecf0f1;        /* Light gray */
  --color-gray: #95a5a6;         /* Gray */
  --color-dark: #2c3e50;         /* Dark */
}
```

### Risk Level Colors

- **LOW** - Green (#27ae60)
- **MEDIUM** - Orange (#f39c12)
- **HIGH** - Red (#e74c3c)

### Status Colors

- **NEW** - Blue (#3498db)
- **REVIEWED** - Green (#27ae60)
- **RESOLVED** - Gray (#95a5a6)

## Testing

### Integration Tests

```bash
# Linux/Mac
./test-integration.sh

# Windows
./test-integration.ps1
```

Tests:
1. API health check
2. Alerts endpoint validation
3. Transaction submission
4. Dashboard connectivity

### Manual Testing

```bash
# 1. Verify API is running
curl http://localhost:8082/api/actuator/health

# 2. Submit test transaction
curl -X POST http://localhost:8080/api/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "amount": 50000,
    "location": "Singapore, SG"
  }'

# 3. Wait 2-4 seconds

# 4. Check alerts
curl http://localhost:8082/api/alerts

# 5. Open dashboard
# http://localhost:3000
```

## Build for Production

### Create Production Build

```bash
npm run build
```

Output: `build/` folder (~500KB gzipped)

### Deploy Options

**Static Hosting (S3, Netlify, etc.)**
```bash
npm run build
# Upload 'build' folder contents
```

**Docker**
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Docker Compose**
```yaml
dashboard:
  build:
    context: ./frontend/dashboard
  ports:
    - "3000:80"
  environment:
    - REACT_APP_API_URL=http://alert-service:8082
  depends_on:
    - alert-service
```

## 🔒 Security

### CORS Configuration

The backend must allow requests from the dashboard URL.

Add to Alert Service (see [CORS_CONFIG_EXAMPLE.java](dashboard/CORS_CONFIG_EXAMPLE.java)):

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("GET", "PUT", "POST")
                    .allowCredentials(true);
            }
        };
    }
}
```

### Best Practices

- ✅ No sensitive data in localStorage
- ✅ HTTPS in production
- ✅ API URL from environment variables
- ✅ Input validation on filters
- ✅ XSS protection (React escapes by default)

## Responsive Breakpoints

```css
/* Desktop (1024px+) */
@media (min-width: 1024px) {
  /* All columns visible */
}

/* Tablet (768px - 1023px) */
@media (max-width: 1023px) {
  /* Optimized spacing */
  /* Hidden reason column */
}

/* Mobile (< 768px) */
@media (max-width: 768px) {
  /* Horizontal scroll tables */
  /* Stacked filters */
  /* Single column layout */
}
```

## Troubleshooting

### "Failed to load alerts"

1. Check API URL in `.env`
2. Verify backend is running: `curl http://localhost:8082/api/actuator/health`
3. Check browser console for CORS errors
4. Verify CORS config on backend

### Port 3000 Already in Use

```bash
PORT=3001 npm start
```

### CORS Error

Setup CORS in backend using [CORS_CONFIG_EXAMPLE.java](dashboard/CORS_CONFIG_EXAMPLE.java)

### Dependencies Not Installed

```bash
rm -rf node_modules package-lock.json
npm install
```

## 🚀 Development Workflow

```bash
# 1. Start backend
cd backend && docker-compose up -d

# 2. Start dashboard
cd frontend/dashboard
npm start

# 3. Open http://localhost:3000

# 4. Make code changes (hot reload)

# 5. Test in browser
```

## 📊 Component Tree

```
App
└── Dashboard
    ├── Header
    │   ├── Title
    │   └── Health Indicator
    ├── Statistics Section
    │   ├── StatCard (Total)
    │   ├── StatCard (High Risk)
    │   ├── StatCard (Medium Risk)
    │   ├── StatCard (Low Risk)
    │   └── StatCard (New)
    ├── Filter Section
    │   └── FilterBar
    │       ├── Search Input
    │       ├── Risk Level Select
    │       ├── Status Select
    │       └── Reset Button
    ├── Table Section
    │   └── AlertsTable
    │       ├── Table Header
    │       ├── Table Body (Rows)
    │       │   ├── Transaction ID
    │       │   ├── User ID
    │       │   ├── Amount
    │       │   ├── RiskBadge
    │       │   ├── StatusBadge
    │       │   ├── Date
    │       │   └── Reason
    │       ├── Loading State
    │       ├── Error State
    │       └── Empty State
    └── Footer
```

## Performance

- Bundle Size: ~500KB (gzipped)
- **Load Time:** <2 seconds (on typical network)
- **Time to Interactive:** <3 seconds
- **API Response:** <500ms (with proper backend)

Optimization techniques:
- Lazy loading (React.lazy)
- Code splitting
- CSS optimization
- Image optimization

## Links

- Backend README: [backend/README.md](../backend/README.md)
- **Installation Guide:** [dashboard/INSTALLATION_GUIDE.md](dashboard/INSTALLATION_GUIDE.md)
- **Project Structure:** [STRUCTURE.md](../STRUCTURE.md)
- **Main README:** [README.md](../README.md)

## � Documentation & Resources

- **[OPERATIONS_DASHBOARD_README.md](../understanding%20project/OPERATIONS_DASHBOARD_README.md)** — Component architecture deep-dive
- **[TRANSACTION_SIMULATOR_GUIDE.md](../understanding%20project/TRANSACTION_SIMULATOR_GUIDE.md)** — Testing & fraud pattern guide
- **[INSTALLATION_GUIDE.md](./dashboard/INSTALLATION_GUIDE.md)** — Setup & troubleshooting
- **[CORS_CONFIG_EXAMPLE.java](./dashboard/CORS_CONFIG_EXAMPLE.java)** — Backend CORS setup

## For Recruiters

React Skills Demonstrated:
- React Hooks (useState, useEffect, useCallback, useContext)
- Custom hooks (usePolling for real-time data)
- Component composition & reusability
- Responsive CSS Grid layout
- HTTP client patterns (Axios with interceptors)
- State management without Redux
- Error handling & user feedback
- Performance optimization (memoization, debouncing)

UI/UX Best Practices:
- Professional color scheme with accessibility
- Mobile-first responsive design
- Loading states & skeleton screens
- Real-time updates with polling
- Graceful error handling
- Comprehensive documentation

Testing Capability:
- Integrated Transaction Simulator for demo scenarios
- Use Normal Mode to validate system baseline
- Use Fraud Mode to trigger real-time alerts
- Stream Mode for load testing

---

**Frontend Version:** 1.0.0  
**Last Updated:** April 9, 2026  
**React Version:** 18.x  
**Status:** Production Ready
