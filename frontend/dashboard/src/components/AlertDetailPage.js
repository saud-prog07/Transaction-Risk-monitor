import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import TransactionTraceView from './TransactionTraceView';
import '../styles/AlertDetailPage.css';

/**
 * AlertDetailPage Component
 * Displays full alert details with investigation workflow
 * - Alert information and history
 * - Investigation notes field
 * - Status update buttons (REVIEWED, FRAUD, SAFE)
 * - Audit trail showing all actions
 * 
 * Security:
 * - API_BASE_URL from environment variables (REACT_APP_API_URL)
 * - Never hardcode localhost or API endpoints in code
 * - All API calls use configured base URL
 */

// Use environment variable or fallback to relative URLs (NOT hardcoded localhost)
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const AlertDetailPage = ({ onClose }) => {
  const { alertId } = useParams();
  const navigate = useNavigate();

  const [alert, setAlert] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [investigationNotes, setInvestigationNotes] = useState('');
  const [investigatedBy, setInvestigatedBy] = useState(localStorage.getItem('userId') || 'current-user');

  // Fetch alert details and audit logs
  useEffect(() => {
    const fetchAlertDetails = async () => {
      try {
        setLoading(true);
        setError(null);

        // Fetch alert details
        const basePath = window.location.pathname.includes('operations') 
          ? '/api/alerts' 
          : '/api/alerts';
        
        const response = await fetch(`${API_BASE_URL}${basePath}/${alertId}`);
        if (!response.ok) {
          throw new Error('Failed to fetch alert details');
        }
        const alertData = await response.json();
        setAlert(alertData);
        setInvestigationNotes(alertData.investigation_notes || '');

        // Fetch audit logs
        const auditResponse = await fetch(`${API_BASE_URL}${basePath}/${alertId}/audit-log?page=0&size=50`);
        if (auditResponse.ok) {
          const auditData = await auditResponse.json();
          setAuditLogs(auditData.content || []);
        }

        setLoading(false);
      } catch (err) {
        console.error('Error fetching alert:', err);
        setError(err.message || 'Failed to load alert details');
        setLoading(false);
      }
    };

    if (alertId) {
      fetchAlertDetails();
    }
  }, [alertId]);

   // Handle status update for REVIEWED
   const handleStatusUpdate = useCallback(async (newStatus) => {
     try {
       setSaving(true);
       setError(null);

       const basePath = '/api/alerts';
       const response = await fetch(`${API_BASE_URL}${basePath}/${alertId}/status`, {
         method: 'PUT',
         headers: {
           'Content-Type': 'application/json',
         },
         body: JSON.stringify({
           status: newStatus,
           notes: investigationNotes,
           investigated_by: investigatedBy,
           metadata: JSON.stringify({
             timestamp: new Date().toISOString(),
             action: newStatus,
           }),
         }),
       });

       if (!response.ok) {
         throw new Error(`Failed to update alert status: ${response.statusText}`);
       }

       const updatedAlert = await response.json();
       setAlert(updatedAlert);
       setInvestigationNotes(updatedAlert.investigation_notes || '');

       // Refresh audit logs
       const auditResponse = await fetch(`${API_BASE_URL}${basePath}/${alertId}/audit-log?page=0&size=50`);
       if (auditResponse.ok) {
         const auditData = await auditResponse.json();
         setAuditLogs(auditData.content || []);
       }

       setSaving(false);
     } catch (err) {
       console.error('Error updating alert status:', err);
       setError(err.message || 'Failed to update alert status');
       setSaving(false);
     }
   }, [alertId, investigationNotes, investigatedBy]);

   // Handle investigation for FRAUD/SAFE
   const handleInvestigation = useCallback(async (decision) => {
     try {
       setSaving(true);
       setError(null);

       const basePath = '/api/alerts';
       const response = await fetch(`${API_BASE_URL}${basePath}/${alertId}/investigate`, {
         method: 'PUT',
         headers: {
           'Content-Type': 'application/json',
         },
         body: JSON.stringify({
           decision: decision, // FRAUD or SAFE
           notes: investigationNotes,
           investigated_by: investigatedBy,
         }),
       });

       if (!response.ok) {
         throw new Error(`Failed to investigate alert: ${response.statusText}`);
       }

       const updatedAlert = await response.json();
       setAlert(updatedAlert);
       setInvestigationNotes(updatedAlert.investigation_notes || '');

       // Refresh audit logs
       const auditResponse = await fetch(`${API_BASE_URL}${basePath}/${alertId}/audit-log?page=0&size=50`);
       if (auditResponse.ok) {
         const auditData = await auditResponse.json();
         setAuditLogs(auditData.content || []);
       }

       setSaving(false);
     } catch (err) {
       console.error('Error investigating alert:', err);
       setError(err.message || 'Failed to investigate alert');
       setSaving(false);
     }
   }, [alertId, investigationNotes, investigatedBy]);

  if (loading) {
    return (
      <div className="alert-detail-page">
        <div className="loading-spinner">Loading alert details...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="alert-detail-page">
        <div className="error-banner">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
          <button className="close-btn" onClick={onClose || (() => navigate(-1))}>×</button>
        </div>
      </div>
    );
  }

  if (!alert) {
    return (
      <div className="alert-detail-page">
        <div className="no-data">Alert not found</div>
      </div>
    );
  }

  const isTerminalStatus = alert.status === 'FRAUD' || alert.status === 'SAFE';
  const statusColor = {
    'NEW': '#3b82f6',
    'REVIEWED': '#f59e0b',
    'FRAUD': '#ef4444',
    'SAFE': '#10b981',
  }[alert.status] || '#6b7280';

  return (
    <div className="alert-detail-page">
      {/* Header */}
      <div className="detail-header">
        <div className="detail-title">
          <h2>Alert Investigation</h2>
          <span className="alert-id">ID: {alert.id}</span>
        </div>
        <button className="close-btn" onClick={onClose || (() => navigate(-1))}>×</button>
      </div>

      {error && (
        <div className="error-banner">
          <span className="error-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* Alert Summary */}
      <div className="alert-summary-section">
        <div className="summary-grid">
          <div className="summary-item">
            <span className="item-label">Risk Level</span>
            <div className="item-value">
              <span className={`risk-badge ${alert.risk_level?.toLowerCase()}`}>
                {alert.risk_level}
              </span>
            </div>
          </div>
          <div className="summary-item">
            <span className="item-label">Status</span>
            <div className="item-value">
              <span 
                className="status-badge"
                style={{ backgroundColor: statusColor }}
              >
                {alert.status}
              </span>
            </div>
          </div>
          <div className="summary-item">
            <span className="item-label">Transaction ID</span>
            <div className="item-value">{alert.transaction_id}</div>
          </div>
          <div className="summary-item">
            <span className="item-label">Created</span>
            <div className="item-value">
              {new Date(alert.created_at).toLocaleString()}
            </div>
          </div>
        </div>

        <div className="alert-reason">
          <span className="reason-label">Reason:</span>
          <p className="reason-text">{alert.reason}</p>
        </div>
      </div>

      {/* Investigation Section */}
      <div className="investigation-section">
        <h3>Investigation Details</h3>

        <div className="investigation-form">
          {/* Investigator Info */}
          <div className="form-group">
            <label htmlFor="investigatedBy">Investigator</label>
            <input
              type="text"
              id="investigatedBy"
              value={investigatedBy}
              onChange={(e) => setInvestigatedBy(e.target.value)}
              placeholder="Your user ID"
              className="form-input"
              disabled={isTerminalStatus}
            />
          </div>

          {/* Investigation Notes */}
          <div className="form-group">
            <label htmlFor="notes">Investigation Notes</label>
            <textarea
              id="notes"
              value={investigationNotes}
              onChange={(e) => setInvestigationNotes(e.target.value)}
              placeholder="Enter your investigation findings and notes here..."
              className="form-textarea"
              rows={6}
              disabled={isTerminalStatus && !investigationNotes}
            />
            <span className="char-count">
              {investigationNotes.length} / 2000 characters
            </span>
          </div>

           {/* Status Buttons */}
           <div className="action-buttons">
             <button
               className="btn btn-reviewed"
               onClick={() => handleStatusUpdate('REVIEWED')}
               disabled={saving || isTerminalStatus}
               title={isTerminalStatus ? 'Cannot change terminal status' : 'Mark as reviewed'}
             >
               {saving ? 'Updating...' : '📋 Mark as Reviewed'}
             </button>
             <button
               className="btn btn-fraud"
               onClick={() => handleInvestigation('FRAUD')}
               disabled={saving}
               title="Confirm this is fraudulent"
             >
               {saving ? 'Updating...' : '🚨 Mark as Fraud'}
             </button>
             <button
               className="btn btn-safe"
               onClick={() => handleInvestigation('SAFE')}
               disabled={saving}
               title="Confirm transaction is safe"
             >
               {saving ? 'Updating...' : '✅ Mark as Safe'}
             </button>
           </div>

          {/* Last Investigated */}
          {alert.investigated_at && (
            <div className="last-investigated">
              <p>
                Last investigated by <strong>{alert.investigated_by || 'Unknown'}</strong> on{' '}
                <strong>{new Date(alert.investigated_at).toLocaleString()}</strong>
              </p>
            </div>
          )}
        </div>
      </div>

       {/* Audit Trail */}
       <div className="audit-trail-section">
         <h3>Audit Trail</h3>
         {auditLogs && auditLogs.length > 0 ? (
           <div className="audit-logs">
             {auditLogs.map((log, idx) => (
               <div key={log.id || idx} className="audit-entry">
                 <div className="entry-header">
                   <span className="action-type">{log.action_type}</span>
                   <span className="timestamp">
                     {new Date(log.action_timestamp).toLocaleString()}
                   </span>
                 </div>
                 <div className="entry-body">
                   <p className="description">{log.description}</p>
                   {log.previous_status && (
                     <div className="status-transition">
                       <span className="status-from">{log.previous_status}</span>
                       <span className="arrow">→</span>
                       <span className="status-to">{log.new_status}</span>
                     </div>
                   )}
                   {log.notes && <p className="notes">{log.notes}</p>}
                   <p className="performed-by">
                     Performed by: <strong>{log.performed_by}</strong>
                   </p>
                 </div>
               </div>
             ))}
           </div>
         ) : (
           <p className="no-logs">No audit logs available</p>
         )}
       </div>

       {/* Transaction Trace View */}
       <div className="trace-section">
         <TransactionTraceView transactionId={alert?.transaction_id} />
       </div>
    </div>
  );
};

export default AlertDetailPage;
