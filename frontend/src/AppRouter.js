import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

// Import components
import OperationsDashboard from './components/OperationsDashboard';
import AlertDetailPage from './components/AlertDetailPage';
import DLQDashboard from './pages/DLQDashboard';
import NotFound from './pages/NotFound';

/**
 * Main App Router Configuration
 * 
 * This file demonstrates how to integrate the AlertDetailPage component
 * with React Router v6 to enable fraud investigation workflow.
 * 
 * Installation Requirements:
 * npm install react-router-dom@6.x
 */

function AppRouter() {
  return (
    <Router>
      <Routes>
        {/* Dashboard - main landing page */}
        <Route path="/" element={<OperationsDashboard />} />
        
        {/* Alert Investigation Workflow */}
        <Route path="/alerts/:alertId" element={<AlertDetailPage />} />
        
        {/* DLQ Dashboard - Dead Letter Queue management (ADMIN only) */}
        <Route path="/admin/dlq" element={<DLQDashboard />} />
        
        {/* Additional routes as needed */}
        {/* <Route path="/transactions" element={<TransactionsList />} />
        <Route path="/settings" element={<Settings />} /> */}
        
        {/* 404 - Not Found */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}

export default AppRouter;

/**
 * INTEGRATION STEPS:
 * 
 * 1. Install React Router (if not already installed):
 *    npm install react-router-dom@6
 * 
 * 2. Replace your existing App.js or create AppRouter.js with the above code
 * 
 * 3. Make sure to have these services/components available:
 *    - components/OperationsDashboard.js
 *    - components/AlertDetailPage.js
 *    - pages/NotFound.js (optional but recommended)
 * 
 * 4. Update your index.js or main app entry point:
 * 
 *    BEFORE:
 *    import App from './App';
 *    ReactDOM.render(<App />, document.getElementById('root'));
 * 
 *    AFTER:
 *    import AppRouter from './AppRouter';
 *    ReactDOM.render(<AppRouter />, document.getElementById('root'));
 * 
 * 5. Verify AlertsPanel component has useNavigate hook:
 *    - Should have: const navigate = useNavigate();
 *    - Should navigate with: navigate(`/alerts/${alert.id}`);
 * 
 * ROUTE STRUCTURE:
 * 
 * GET  /                    → OperationsDashboard (main view)
 * GET  /alerts/:alertId     → AlertDetailPage (investigation view)
 * GET  /any-other-path      → NotFound page
 * 
 * EXAMPLE USAGE:
 * 
 * In AlertsPanel.js:
 * 
 *   const handleViewDetails = (alertId) => {
 *     navigate(`/alerts/${alertId}`);
 *   };
 * 
 *   <button onClick={() => handleViewDetails(alert.id)}>
 *     🔍 Investigate
 *   </button>
 * 
 * In AlertDetailPage.js:
 * 
 *   const { alertId } = useParams();  // Gets from URL /alerts/:alertId
 *   const navigate = useNavigate();
 *   
 *   // Navigate back to dashboard
 *   navigate('/');
 */
