import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import TransactionSimulator from './TransactionSimulator';
import '../styles/GuidedDemoFlow.css';

/**
 * GuidedDemoFlow Component
 * Provides a step-by-step guided demonstration of the transaction risk monitoring system
 * 
 * Steps:
 * 1. Generate transactions
 * 2. View processing
 * 3. See alerts
 * 4. Investigate
 * 
 * Highlights each step visually and makes system easy to demonstrate
 */
const GuidedDemoFlow = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [isDemoRunning, setIsDemoRunning] = useState(false);
  const [demoStatus, setDemoStatus] = useState('ready');
  const [highlightedElement, setHighlightedElement] = useState(null);
  const simulatorRef = useRef(null);
  const dashboardRef = useRef(null);
  const alertsRef = useRef(null);

  // Step configurations
  const steps = [
    {
      id: 1,
      title: 'Generate Transactions',
      description: 'Use the transaction simulator to create normal and fraudulent transactions',
      action: () => {
        if (simulatorRef.current) {
          simulatorRef.current.startDemoMode();
        }
        setHighlightedElement('transaction-simulator');
        setDemoStatus('generating');
      },
      completed: false
    },
    {
      id: 2,
      title: 'View Processing',
      description: 'Watch transactions being processed in real-time through the system',
      action: () => {
        setHighlightedElement('dashboard');
        setDemoStatus('processing');
      },
      completed: false
    },
    {
      id: 3,
      title: 'See Alerts',
      description: 'Monitor flagged transactions appearing in the alerts dashboard',
      action: () => {
        setHighlightedElement('alerts-table');
        setDemoStatus('alerting');
      },
      completed: false
    },
    {
      id: 4,
      title: 'Investigate Alerts',
      description: 'Review and investigate alerts to determine if they are fraud or safe',
      action: () => {
        setHighlightedElement('alert-detail');
        setDemoStatus('investigating');
      },
      completed: false
    }
  ];

  // Start demo when component mounts
  useEffect(() => {
    const startDemo = async () => {
      setIsDemoRunning(true);
      await runDemoStep(1);
    };
    
    startDemo();
  }, []);

  // Run demo step by step
  const runDemoStep = async (stepId) => {
    if (stepId > steps.length) {
      setDemoStatus('completed');
      setIsDemoRunning(false);
      return;
    }

    const step = steps[stepId - 1];
    step.completed = true;
    setCurrentStep(stepId);
    
    // Execute step action
    step.action();
    
    // Wait for user to complete step (or auto-advance after delay for demo)
    await new Promise(resolve => setTimeout(resolve, 8000)); // 8 seconds per step
    
    // Auto-advance to next step
    runDemoStep(stepId + 1);
  };

  // Manual step navigation
  const goToStep = (stepId) => {
    setCurrentStep(stepId);
    setHighlightedElement(steps[stepId - 1]?.highlightedElement || null);
    steps[stepId - 1]?.action?.();
  };

  const nextStep = () => {
    if (currentStep < steps.length) {
      goToStep(currentStep + 1);
    }
  };

  const prevStep = () => {
    if (currentStep > 1) {
      goToStep(currentStep - 1);
    }
  };

  // Handle demo completion
  const handleDemoComplete = () => {
    setIsDemoRunning(false);
    setDemoStatus('completed');
    setHighlightedElement(null);
  };

  if (!isDemoRunning && demoStatus !== 'completed') {
    return (
      <div className="guided-demo-overlay">
        <div className="guided-demo-container">
          <div className="guided-demo-header">
            <h2>🎯 Transaction Risk Monitoring System Demo</h2>
            <p className="guided-demo-subtitle">Follow along to see how the system detects and investigates fraudulent transactions</p>
          </div>
          
          <div className="guided-demo-progress">
            <div className="progress-bar">
              <div 
                className="progress-fill" 
                style={{ width: `${((currentStep - 1) / steps.length) * 100}%` }}
              ></div>
            </div>
            <div className="progress-text">
              Step {currentStep} of {steps.length}
            </div>
          </div>
          
          <div className="guided-demo-content">
            <div className="demo-step-current">
              <h3>{steps[currentStep - 1].title}</h3>
              <p>{steps[currentStep - 1].description}</p>
            </div>
            
            <div className="demo-steps-list">
              {steps.map((step, index) => (
                <div 
                  key={index} 
                  className={`demo-step-item ${index + 1 === currentStep ? 'active' : ''} ${step.completed ? 'completed' : ''}`}
                  onClick={() => goToStep(index + 1)}
                >
                  <div className="step-number">{index + 1}</div>
                  <div className="step-info">
                    <h4>{step.title}</h4>
                    <p>{step.description}</p>
                  </div>
                  {step.completed && (
                    <span className="step-check">✓</span>
                  )}
                </div>
              ))}
            </div>
          </div>
          
          <div className="guided-demo-controls">
            <button 
              className="btn btn-outline" 
              onClick={prevStep} 
              disabled={currentStep === 1}
            >
              ← Previous
            </button>
            <button 
              className="btn btn-primary" 
              onClick={nextStep} 
              disabled={currentStep === steps.length}
            >
              {currentStep === steps.length ? 'Finish Demo' : 'Next Step →'}
            </button>
            <button 
              className="btn btn-danger" 
              onClick={handleDemoComplete}
            >
              Exit Demo
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="guided-demo-wrapper">
      {/* Demo Overlay */}
      {isDemoRunning && (
        <div className="guided-demo-overlay">
          <div className="guided-demo-container">
            <div className="guided-demo-header">
              <h2>🎯 Transaction Risk Monitoring System Demo</h2>
              <p className="guided-demo-subtitle">Follow along to see how the system detects and investigates fraudulent transactions</p>
              <button 
                className="btn btn-sm btn-outline" 
                onClick={handleDemoComplete}
                style={{ position: 'absolute', top: '10px', right: '10px' }}
              >
                Exit
              </button>
            </div>
            
            <div className="guided-demo-progress">
              <div className="progress-bar">
                <div 
                  className="progress-fill" 
                  style={{ width: `${((currentStep - 1) / steps.length) * 100}%` }}
                ></div>
              </div>
              <div className="progress-text">
                Step {currentStep} of {steps.length}
              </div>
            </div>
            
            <div className="guided-demo-content">
              <div className="demo-step-current">
                <h3>{steps[currentStep - 1].title}</h3>
                <p>{steps[currentStep - 1].description}</p>
              </div>
              
              <div className="demo-steps-list">
                {steps.map((step, index) => (
                  <div 
                    key={index} 
                    className={`demo-step-item ${index + 1 === currentStep ? 'active' : ''} ${step.completed ? 'completed' : ''}`}
                  >
                    <div className="step-number">{index + 1}</div>
                    <div className="step-info">
                      <h4>{step.title}</h4>
                      <p>{step.description}</p>
                    </div>
                    {step.completed && (
                      <span className="step-check">✓</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
            
            <div className="guided-demo-controls">
              <button 
                className="btn btn-outline" 
                onClick={prevStep} 
                disabled={currentStep === 1}
              >
                ← Previous
              </button>
              <button 
                className="btn btn-primary" 
                onClick={nextStep} 
                disabled={currentStep === steps.length}
              >
                {currentStep === steps.length ? 'Finish Demo' : 'Next Step →'}
              </button>
            </div>
          </div>
        </div>
      )}
      
      {/* Main Content */}
      <div className="guided-demo-main">
        {/* Transaction Simulator */}
        <div 
          ref={simulatorRef}
          className={`${highlightedElement === 'transaction-simulator' ? 'highlighted' : ''} transaction-simulator-container`}
        >
          <TransactionSimulator 
            isDemoMode={isDemoRunning} 
            currentStep={currentStep}
          />
        </div>
        
        {/* Dashboard Section */}
        <div 
          ref={dashboardRef}
          className={`${highlightedElement === 'dashboard' ? 'highlighted' : ''} dashboard-container`}
        >
          <div className="dashboard-header">
            <h3>📊 System Dashboard</h3>
            <p>Watch transactions being processed in real-time</p>
          </div>
          <div className="dashboard-content">
            {/* Placeholder for dashboard metrics */}
            <div className="dashboard-placeholder">
              <div className="metric-card">
                <h4>Transactions Processed</h4>
                <div className="metric-value" id="transactions-processed">0</div>
              </div>
              <div className="metric-card">
                <h4>Alerts Generated</h4>
                <div className="metric-value" id="alerts-generated">0</div>
              </div>
              <div className="metric-card">
                <h4>Processing Rate</h4>
                <div className="metric-value" id="processing-rate">0 tx/sec</div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Alerts Section */}
        <div 
          ref={alertsRef}
          className={`${highlightedElement === 'alerts-table' ? 'highlighted' : ''} alerts-container`}
        >
          <div className="alerts-header">
            <h3>🚨 Flagged Transactions</h3>
            <p>Monitor suspicious transactions as they appear</p>
          </div>
          <div className="alerts-content">
            {/* Placeholder for alerts table */}
            <div className="alerts-placeholder">
              <div className="alert-item" id="alert-1">
                <div className="alert-info">
                  <span className="alert-id">ALT-001</span>
                  <span className="alert-risk risk-high">HIGH</span>
                </div>
                <div className="alert-details">
                  <p>Transaction to UNKNOWN MERCHANT for $12,450.00</p>
                  <p className="alert-reason">High amount + Rapid sequence</p>
                </div>
                <div className="alert-status status-new">NEW</div>
              </div>
              <div className="alert-item" id="alert-2">
                <div className="alert-info">
                  <span className="alert-id">ALT-002</span>
                  <span className="alert-risk risk-medium">MEDIUM</span>
                </div>
                <div className="alert-details">
                  <p>Transaction to GAS STATION for $85.00</p>
                  <p className="alert-reason">Unusual location</p>
                </div>
                <div className="alert-status status-reviewed">REVIEWED</div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Alert Detail Section */}
        <div 
          className={`${highlightedElement === 'alert-detail' ? 'highlighted' : ''} alert-detail-container`}
        >
          <div className="alert-detail-header">
            <h3>🔍 Alert Investigation</h3>
            <p>Review details and make fraud/safe determination</p>
          </div>
          <div className="alert-detail-content">
            {/* Placeholder for alert detail view */}
            <div className="alert-detail-form">
              <div className="form-group">
                <label>Alert ID:</label>
                <span className="form-value">ALT-001</span>
              </div>
              <div className="form-group">
                <label>Transaction ID:</label>
                <span className="form-value">TXN-DEMO-001</span>
              </div>
              <div className="form-group">
                <label>Amount:</label>
                <span className="form-value">$12,450.00</span>
              </div>
              <div className="form-group">
                <label>Risk Level:</label>
                <span className="form-value risk-high">HIGH</span>
              </div>
              <div className="form-group">
                <label>Reason:</label>
                <span className="form-value">High amount + Rapid sequence</span>
              </div>
              <div className="form-group">
                <label>Investigation Notes:</label>
                <textarea 
                  placeholder="Enter your investigation findings..."
                  className="investigation-notes"
                ></textarea>
              </div>
              <div className="form-group">
                <label>Investigated By:</label>
                <input 
                  type="text" 
                  placeholder="Your user ID"
                  className="investigator-input"
                  value="demo-user"
                />
              </div>
              <div className="alert-actions">
                <button 
                  className="btn btn-outline btn-investigate"
                  onClick={() => alert('Investigation submitted!')}
                >
                  📝 Submit Investigation
                </button>
                <button 
                  className="btn btn-danger btn-fraud"
                  onClick={() => alert('Marked as FRAUD!')}
                >
                  🚨 Mark as Fraud
                </button>
                <button 
                  className="btn btn-success btn-safe"
                  onClick={() => alert('Marked as SAFE!')}
                >
                  ✅ Mark as Safe
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GuidedDemoFlow;