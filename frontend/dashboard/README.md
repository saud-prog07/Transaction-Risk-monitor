# Transaction Risk Monitoring Dashboard

Real-time fraud detection dashboard for the Transaction Risk Monitoring System built with React.js.

## 🎯 Features

✅ **Real-Time Statistics** - Display total alerts, flagged transactions by risk level  
✅ **Risk Level Indicators** - Color-coded badges (LOW, MEDIUM, HIGH) with risk scores  
✅ **Advanced Filtering** - Filter by risk level and status (NEW, REVIEWED, RESOLVED)  
✅ **Search Functionality** - Search by User ID or Transaction ID  
✅ **Responsive Design** - Mobile-friendly UI that works on all devices  
✅ **Status Management** - Update alert statuses directly from dashboard  
✅ **API Health Monitoring** - Real-time connection status  
✅ **Professional UI** - Clean, minimal, enterprise-grade design  

## 🏗️ Project Structure

```
dashboard/
├── public/
│   └── index.html                 # Entry HTML file
├── src/
│   ├── components/
│   │   ├── Dashboard.js          # Main dashboard container
│   │   ├── StatCard.js           # Statistics cards
│   │   ├── AlertsTable.js        # Alerts table display
│   │   ├── FilterBar.js          # Filtering & search controls
│   │   ├── RiskBadge.js          # Risk level indicator
│   │   └── StatusBadge.js        # Alert status indicator
│   ├── services/
│   │   ├── apiService.js         # Axios API client
│   │   └── utils.js              # Utility functions
│   ├── styles/
│   │   ├── index.css             # Global styles
│   │   ├── Dashboard.css         # Dashboard container styles
│   │   ├── StatCard.css          # StatCard component styles
│   │   ├── AlertsTable.css       # Table styles
│   │   ├── FilterBar.css         # Filter bar styles
│   │   ├── RiskBadge.css         # Risk badge styles
│   │   └── StatusBadge.css       # Status badge styles
│   ├── App.js                    # Root component
│   └── index.js                  # React entry point
├── package.json                  # Dependencies
└── .env.example                  # Environment template
```

## 📋 Prerequisites

- **Node.js** 14.0 or higher
- **npm** 6.0 or higher
- **Alert Service API** running on `http://localhost:8082`

## 🚀 Getting Started

### 1. Clone and Install

```bash
# Navigate to dashboard directory
cd dashboard

# Install dependencies
npm install
```

### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env and set your API URL
REACT_APP_API_URL=http://localhost:8082
```

### 3. Start Development Server

```bash
npm start
```

The dashboard will open at `http://localhost:3000`

### 4. Build for Production

```bash
npm run build
```

This creates an optimized production build in the `build/` directory.

## 🔌 API Integration

The dashboard connects to the Alert Service API. Ensure your backend is running with these endpoints:

### Required Endpoints

**Get Alerts** (with pagination & filtering)
```
GET /api/alerts?page=0&size=20&riskLevel=HIGH&status=NEW
```

**Update Alert Status**
```
PUT /api/alerts/{alertId}/status
Body: { "status": "REVIEWED" }
```

**Health Check**
```
GET /api/actuator/health
```

## 📊 Component Details

### StatCard

Displays statistics with icon and color coding.

```jsx
<StatCard
  title="High Risk"
  value={45}
  icon="🔴"
  color="danger"
  subtitle="Critical alerts"
/>
```

### RiskBadge

Color-coded risk level indicator (LOW, MEDIUM, HIGH).

```jsx
<RiskBadge level="HIGH" score={87.5} />
```

### FilterBar

Search and filter controls with real-time updates.

```jsx
<FilterBar
  onFilterChange={(filters) => console.log(filters)}
  onSearchChange={(search) => console.log(search)}
/>
```

### AlertsTable

Displays alerts in a responsive table with status management.

```jsx
<AlertsTable
  alerts={alerts}
  loading={false}
  error={null}
  onRefresh={() => loadAlerts()}
  onStatusUpdate={(id, status) => updateAlert(id, status)}
/>
```

## 🎨 Customization

### Colors

Edit CSS variables in `src/styles/index.css`:

```css
:root {
  --color-primary: #2c3e50;
  --color-secondary: #3498db;
  --color-success: #27ae60;
  --color-warning: #f39c12;
  --color-danger: #e74c3c;
}
```

### API Base URL

Set in `.env`:

```
REACT_APP_API_URL=http://your-api-host:8082
```

### Refresh Interval

Set in `.env`:

```
REACT_APP_REFRESH_INTERVAL=30000  # 30 seconds
```

## 🔍 Filtering & Search

The dashboard supports:

- **Search by User ID** - Partial match search
- **Search by Transaction ID** - Partial match search
- **Filter by Risk Level** - LOW, MEDIUM, HIGH
- **Filter by Status** - NEW, REVIEWED, RESOLVED

All filters work independently and combine for compound queries.

## 📱 Responsive Design

- **Desktop** (1024px+) - Full layout with all columns visible
- **Tablet** (768px - 1023px) - Optimized spacing, hidden reason column
- **Mobile** (< 768px) - Horizontal scroll for tables, stacked filters

## 🐛 Debugging

Enable debug logging by opening browser console:

```javascript
// Check API health
fetch('http://localhost:8082/api/actuator/health')
  .then(r => r.json())
  .then(console.log);
```

## 📦 Production Deployment

### Build

```bash
npm run build
```

### Deploy to Static Hosting

```bash
# AWS S3
aws s3 sync build/ s3://your-bucket-name/

# GitHub Pages
npm install --save-dev gh-pages
# Add to package.json:
# "homepage": "https://yourusername.github.io/repo-name",
# "predeploy": "npm run build",
# "deploy": "gh-pages -d build"
npm run deploy
```

### Docker

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

## 🧪 Testing

Add tests (optional):

```bash
npm test
```

## 🔐 Security

- API requests use HTTPS in production
- No sensitive data stored in localStorage
- CORS configured on backend
- Input validation on all filters

## 📝 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REACT_APP_API_URL` | Backend API URL | `http://localhost:8082` |
| `REACT_APP_APP_NAME` | App display name | `Transaction Risk Monitor` |
| `REACT_APP_REFRESH_INTERVAL` | Auto-refresh interval (ms) | `30000` |

## 🚨 Troubleshooting

### API Connection Error

```
Failed to load alerts. Please check your connection.
```

**Solution:** Ensure Alert Service is running and `REACT_APP_API_URL` is correct.

### CORS Error

```
Access to XMLHttpRequest blocked by CORS policy
```

**Solution:** Configure CORS on your backend:

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

### Components Not Loading

Clear browser cache:

```bash
npm start -- --reset-cache
```

## 📞 Support

For issues or questions:
- Check `.env` configuration
- Verify backend API is running
- Check browser console for errors
- Review network requests in DevTools

## 📄 License

Production-ready implementation for the Transaction Risk Monitoring System.

---

**Created:** April 2026  
**Version:** 1.0.0  
**Status:** Production Ready
