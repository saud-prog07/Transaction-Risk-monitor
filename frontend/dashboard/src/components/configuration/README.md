# Risk Analyzer Configuration Management System

Comprehensive configuration management system for risk analyzers, thresholds, and monitoring metrics in the Real-Time Transaction Risk Monitoring System.

## 📋 Overview

This package provides a complete solution for managing risk analyzer configurations with:

- **Dynamic Analyzer Configuration**: Enable/disable analyzers, adjust weights and parameters
- **Risk Threshold Management**: Configure risk level thresholds (Low, Medium, High, Critical)
- **Real-time Metrics**: Monitor analyzer performance and health status
- **Form Management**: Robust form handling with validation
- **API Integration**: Service layer for backend communication
- **Caching**: Smart caching with configurable expiry
- **Export/Import**: Configuration backup and restore

## 🗂️ File Structure

```
frontend/dashboard/src/
├── components/
│   └── configuration/
│       ├── RiskAnalyzerConfigPanel.jsx      # Main config panel
│       ├── RiskThresholdConfig.jsx          # Threshold management
│       ├── RiskAnalyzerMetricsPanel.jsx     # Metrics display
│       └── index.js                         # Component exports
├── hooks/
│   ├── useRiskConfig.js                     # Configuration hook
│   ├── useRiskConfigForm.js                 # Form management hook
│   └── index.js                             # Hook exports
├── services/
│   ├── ConfigurationService.js              # API service
│   └── index.js                             # Service exports
└── styles/
    ├── RiskAnalyzerConfig.css               # Config panel styles
    └── RiskThresholdConfig.css              # Threshold styles
```

## 🚀 Quick Start

### 1. Basic Usage with useRiskConfig Hook

```jsx
import { useRiskConfig } from '../hooks';
import { RiskAnalyzerConfigPanel } from '../components/configuration';

function ConfigDashboard() {
  const {
    analyzers,
    thresholds,
    metrics,
    loading,
    error,
    updateAnalyzer,
    updateThresholds,
    fetchMetrics,
  } = useRiskConfig({
    autoFetch: true,
    refreshInterval: 30000, // Refresh every 30 seconds
  });

  const handleAnalyzerUpdate = async (analyzerId, config) => {
    try {
      await updateAnalyzer(analyzerId, config);
      // Success - config automatically updates
    } catch (err) {
      console.error('Update failed:', err);
    }
  };

  return (
    <RiskAnalyzerConfigPanel
      analyzers={analyzers}
      thresholds={thresholds}
      metrics={metrics}
      isLoading={loading}
      error={error}
      onUpdate={handleAnalyzerUpdate}
      onRefresh={fetchMetrics}
    />
  );
}
```

### 2. Form Management with useRiskConfigForm

```jsx
import { useRiskConfigForm } from '../hooks';

function AnalyzerForm() {
  const initialValues = {
    name: 'Behavioral Analyzer',
    weight: 0.6,
    threshold: 75,
    enabled: true,
  };

  const validate = (values) => {
    const errors = {};
    if (!values.name) errors.name = 'Name is required';
    if (values.weight < 0 || values.weight > 1) {
      errors.weight = 'Weight must be 0-1';
    }
    if (values.threshold < 0 || values.threshold > 100) {
      errors.threshold = 'Threshold must be 0-100';
    }
    return errors;
  };

  const handleSubmit = async (values) => {
    // Submit to API
    await configService.updateAnalyzer('analyzer-id', values);
  };

  const form = useRiskConfigForm(initialValues, handleSubmit, validate);

  return (
    <form onSubmit={form.handleSubmit}>
      <input
        {...form.getFieldProps('name')}
        placeholder="Analyzer name"
      />
      {form.errors.name && <span>{form.errors.name}</span>}

      <input
        {...form.getFieldProps('weight')}
        type="number"
        step="0.1"
        min="0"
        max="1"
      />
      {form.errors.weight && <span>{form.errors.weight}</span>}

      <button type="submit" disabled={form.isSubmitting}>
        {form.isSubmitting ? 'Saving...' : 'Save'}
      </button>
    </form>
  );
}
```

### 3. Configuration Service Direct Usage

```jsx
import { configService } from '../services';

// Get all analyzers
const analyzers = await configService.getAnalyzers();

// Update single analyzer
const updated = await configService.updateAnalyzer('analyzer-id', {
  weight: 0.8,
  enabled: true,
});

// Batch update
await configService.batchUpdateAnalyzers([
  { id: 'analyzer-1', config: { weight: 0.6 } },
  { id: 'analyzer-2', config: { weight: 0.4 } },
]);

// Get thresholds
const thresholds = await configService.getThresholds();

// Update thresholds
await configService.updateThresholds([
  {
    name: 'Default',
    low: 20,
    medium: 40,
    high: 60,
    critical: 80,
  },
]);

// Get metrics
const metrics = await configService.getMetrics();
const analyzerMetrics = await configService.getMetrics('analyzer-id');

// Export configuration
await configService.exportConfig(); // Downloads as JSON file

// Import configuration
const file = /* user selected file */;
await configService.importConfig(file);

// Get audit trail
const auditTrail = await configService.getAuditTrail({
  limit: 50,
  offset: 0,
  analyzerId: 'analyzer-id',
});

// Revert to previous version
await configService.revertConfig('version-id');
```

## 🔧 Components

### RiskAnalyzerConfigPanel

Main configuration panel for managing risk analyzers.

**Props:**
- `analyzers` (Array): Array of analyzer configurations
- `thresholds` (Array, optional): Risk thresholds
- `metrics` (Object, optional): Analyzer metrics
- `isLoading` (Boolean): Loading state
- `error` (String): Error message
- `onUpdate` (Function): Callback on analyzer update
- `onReset` (Function): Callback on analyzer reset
- `onRefresh` (Function): Callback to refresh metrics

**Features:**
- Enable/disable analyzers with toggle switches
- Edit analyzer parameters (weight, threshold, etc.)
- View real-time metrics
- Batch operations
- Form validation with error messages
- Audit trail integration

**Example:**

```jsx
<RiskAnalyzerConfigPanel
  analyzers={analyzers}
  thresholds={thresholds}
  metrics={metrics}
  isLoading={loading}
  error={error}
  onUpdate={(id, config) => updateAnalyzer(id, config)}
  onRefresh={() => fetchMetrics()}
/>
```

### RiskThresholdConfig

Component for managing risk level thresholds.

**Props:**
- `thresholds` (Array): Threshold configurations
- `onSave` (Function): Callback on save
- `onCancel` (Function): Callback on cancel
- `isLoading` (Boolean): Loading state
- `error` (String): Error message

**Features:**
- Visual threshold representation
- Add/remove thresholds
- Validation (ordering: low < medium < high < critical)
- Edit mode with form inputs
- Display mode with visualizations

**Example:**

```jsx
<RiskThresholdConfig
  thresholds={thresholds}
  onSave={(newThresholds) => updateThresholds(newThresholds)}
  onCancel={() => resetForm()}
/>
```

### RiskAnalyzerMetricsPanel

Real-time metrics and performance monitoring for analyzers.

**Props:**
- `analyzers` (Array): Analyzer configurations
- `metrics` (Object): Metrics data
- `isLoading` (Boolean): Loading state
- `error` (String): Error message
- `onRefresh` (Function): Callback to refresh metrics

**Features:**
- Overview tab with aggregate statistics
- Analyzer details with expandable items
- Health status with health scores
- Performance metrics (latency, error rates)
- Charts and visualizations
- Real-time updates

**Example:**

```jsx
<RiskAnalyzerMetricsPanel
  analyzers={analyzers}
  metrics={metrics}
  isLoading={loading}
  error={error}
  onRefresh={() => fetchMetrics()}
/>
```

## 🎣 Custom Hooks

### useRiskConfig

Comprehensive hook for managing risk configurations.

**Options:**
- `refreshInterval` (Number): Auto-refresh interval in ms (0 to disable)
- `autoFetch` (Boolean): Automatically fetch on mount

**Returns:**

```javascript
{
  // State
  analyzers,           // Array of analyzer configs
  thresholds,          // Array of threshold configs
  metrics,             // Metrics data object
  loading,             // Loading state
  error,               // Error message
  lastUpdated,         // Last update timestamp

  // Analyzer operations
  fetchAnalyzers,      // Fetch all analyzers
  updateAnalyzer,      // Update single analyzer
  batchUpdateAnalyzers,// Batch update analyzers
  resetAnalyzer,       // Reset to default
  getAnalyzer,         // Get specific analyzer

  // Threshold operations
  fetchThresholds,     // Fetch thresholds
  updateThresholds,    // Update thresholds

  // Metrics operations
  fetchMetrics,        // Fetch metrics
  getAnalyzerMetrics,  // Get specific analyzer metrics

  // Configuration operations
  fetchAll,            // Fetch all configs
  validateConfig,      // Validate configuration
  exportConfig,        // Export to file
  importConfig,        // Import from file

  // Utilities
  clearError,          // Clear error state
}
```

**Example:**

```jsx
const {
  analyzers,
  loading,
  error,
  updateAnalyzer,
  fetchMetrics,
} = useRiskConfig({
  refreshInterval: 30000,
  autoFetch: true,
});
```

### useRiskConfigForm

Form state management hook for configuration forms.

**Parameters:**
- `initialValues` (Object): Initial form values
- `onSubmit` (Function): Submit callback
- `validate` (Function): Validation function
- `options` (Object): Form options
  - `validateOnChange` (Boolean): Validate on field change
  - `validateOnBlur` (Boolean): Validate on field blur

**Returns:**

```javascript
{
  // Form state
  values,              // Current form values
  errors,              // Field errors
  touched,             // Touched fields
  isSubmitting,        // Submitting state
  isDirty,             // Form modified
  isValid,             // Form is valid

  // Form methods
  handleChange,        // Change handler
  handleBlur,          // Blur handler
  handleSubmit,        // Submit handler
  resetForm,           // Reset form

  // Field methods
  setFieldValue,       // Set single field
  setFieldError,       // Set field error
  setFieldTouched,     // Mark field touched
  setValues,           // Set multiple fields
  setErrors,           // Set multiple errors

  // Getters
  getFieldProps,       // Get field props object
  getFieldMeta,        // Get field metadata

  // Utilities
  hasChanges,          // Check if form changed
}
```

**Example:**

```jsx
const form = useRiskConfigForm(
  initialValues,
  handleSubmit,
  validate,
  { validateOnChange: true }
);
```

## 🔌 Service Layer

### ConfigurationService

API service for backend communication.

**Methods:**

```javascript
// Analyzers
getAnalyzers()                    // Get all analyzers
getAnalyzer(analyzerId)           // Get specific analyzer
updateAnalyzer(id, config)        // Update analyzer
batchUpdateAnalyzers(updates)     // Batch update
resetAnalyzer(analyzerId)         // Reset to default

// Thresholds
getThresholds()                   // Get all thresholds
updateThresholds(thresholds)      // Update thresholds

// Metrics
getMetrics(analyzerId)            // Get metrics

// Configuration
validateConfig(config)            // Validate configuration
exportConfig()                    // Export as file
importConfig(file)                // Import from file
getAuditTrail(options)            // Get audit trail
revertConfig(version)             // Revert to version

// Cache management
clearCache()                      // Clear all cache
setCacheExpiry(ms)                // Set cache expiry time
```

**Connection Details:**

- Base URL: `/api` (configurable)
- Returns JSON responses
- Automatic error handling
- Built-in caching with 5-minute expiry (configurable)

## 🎨 Styling

### CSS Files

- **RiskAnalyzerConfig.css**: Main configuration panel styles
- **RiskThresholdConfig.css**: Threshold configuration styles
- **RiskAnalyzerMetricsPanel.css**: Metrics panel styles

### Design Features

- **Dark theme** with gradient backgrounds
- **Responsive design** for mobile/tablet/desktop
- **Smooth animations** and transitions
- **Accessibility** features
- **Color-coded** status indicators
- **Interactive visualizations**

### Customization

```jsx
// Override default styles
.risk-analyzer-config {
  background: linear-gradient(135deg, #0f172a 0%, #1a1f3a 100%);
  /* Custom styles */
}
```

## 📊 Data Structures

### Analyzer Configuration

```javascript
{
  id: 'behavioral-analyzer',
  name: 'Behavioral Analyzer',
  description: 'Analyzes transaction behavior patterns',
  type: 'behavioral',
  icon: '🎯',
  enabled: true,
  weight: 0.6,
  threshold: 75,
  config: {
    timeWindow: 3600,
    minSamples: 10,
    deviationFactor: 2.5,
  },
}
```

### Threshold Configuration

```javascript
{
  name: 'Default Risk Thresholds',
  low: 20,
  medium: 40,
  high: 60,
  critical: 80,
}
```

### Analyzer Metrics

```javascript
{
  'behavioral-analyzer': {
    enabled: true,
    status: 'healthy',
    requestCount: 15420,
    errorCount: 12,
    avgExecutionTime: 45.5,
    minExecutionTime: 10.2,
    maxExecutionTime: 250.3,
    p95Latency: 120.5,
    p99Latency: 180.7,
    uptime: 99.92,
    lastUpdated: '2024-01-15T10:30:00Z',
  },
}
```

## 🔐 Security Considerations

- API calls use HTTPS in production
- CORS configured for safe cross-origin requests
- Configuration validated before submission
- Audit trail tracks all changes
- Read-only metrics view prevents accidental modifications
- Form validation prevents invalid configurations

## 🐛 Error Handling

All components include comprehensive error handling:

```jsx
{error && (
  <div className="config-error">
    <span className="error-icon">⚠️</span>
    <span className="error-text">{error}</span>
  </div>
)}
```

Errors are:
- Displayed in UI with clear messages
- Logged to console for debugging
- Returned in hook state for handling
- Cleared when operations succeed

## 📱 Responsive Design

All components are fully responsive:

- **Desktop**: Full grid layout with all features
- **Tablet**: 2-column grid, optimized spacing
- **Mobile**: Single column, touch-friendly buttons

## 🎯 Best Practices

1. **Use hooks for state management**: Prefer `useRiskConfig` over direct service calls
2. **Validate before submit**: Always validate configurations before saving
3. **Handle errors gracefully**: Show user-friendly error messages
4. **Cache appropriately**: Use service caching to reduce API calls
5. **Audit changes**: Track configuration modifications for compliance
6. **Export regularly**: Export configurations for backup and disaster recovery

## 📚 Examples

### Complete Configuration Management Page

```jsx
import { useRiskConfig } from '../hooks';
import { 
  RiskAnalyzerConfigPanel, 
  RiskThresholdConfig, 
  RiskAnalyzerMetricsPanel 
} from '../components/configuration';

export default function ConfigurationPage() {
  const {
    analyzers,
    thresholds,
    metrics,
    loading,
    error,
    updateAnalyzer,
    updateThresholds,
    fetchMetrics,
    clearError,
  } = useRiskConfig({
    autoFetch: true,
    refreshInterval: 30000,
  });

  return (
    <div className="configuration-page">
      {error && (
        <div className="error-banner">
          {error}
          <button onClick={clearError}>✕</button>
        </div>
      )}

      <h1>Risk Configuration Management</h1>

      <RiskAnalyzerConfigPanel
        analyzers={analyzers}
        thresholds={thresholds}
        metrics={metrics}
        isLoading={loading}
        error={error}
        onUpdate={updateAnalyzer}
        onRefresh={fetchMetrics}
      />

      <RiskThresholdConfig
        thresholds={thresholds}
        onSave={updateThresholds}
      />

      <RiskAnalyzerMetricsPanel
        analyzers={analyzers}
        metrics={metrics}
        isLoading={loading}
        onRefresh={fetchMetrics}
      />
    </div>
  );
}
```

## 🤝 Integration with Backend

The system expects the following API endpoints:

```
GET    /api/config/analyzers              # Get all analyzers
GET    /api/config/analyzers/:id          # Get specific analyzer
PUT    /api/config/analyzers/:id          # Update analyzer
PUT    /api/config/analyzers/batch        # Batch update
POST   /api/config/analyzers/:id/reset    # Reset analyzer

GET    /api/config/thresholds             # Get thresholds
PUT    /api/config/thresholds             # Update thresholds

GET    /api/metrics/analyzers             # Get all metrics
GET    /api/metrics/analyzers/:id         # Get analyzer metrics

POST   /api/config/validate               # Validate configuration
GET    /api/config/export                 # Export configuration
POST   /api/config/import                 # Import configuration
GET    /api/config/audit                  # Get audit trail
POST   /api/config/revert/:version        # Revert to version
```

## 📝 License

Part of the Real-Time Transaction Risk Monitoring System

## 👥 Support

For issues, questions, or contributions, contact the development team.
