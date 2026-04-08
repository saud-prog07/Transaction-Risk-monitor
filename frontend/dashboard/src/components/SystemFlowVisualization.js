import React, { useState, useEffect } from 'react';
import '../styles/SystemFlowVisualization.css';

/**
 * SystemFlowVisualization Component
 * Visualizes the data flow through the transaction monitoring system:
 * Transaction → Producer → MQ → Risk Engine → Alert Service
 */
const SystemFlowVisualization = () => {
  const [activeStage, setActiveStage] = useState(null);
  const [isAnimating, setIsAnimating] = useState(false);

  // Define the system flow stages
  const stages = [
    { 
      id: 'transaction', 
      label: 'Transaction', 
      icon: '💳',
      description: 'Incoming transaction data'
    },
    { 
      id: 'producer', 
      label: 'Producer', 
      icon: '📡',
      description: 'Transaction producer service'
    },
    { 
      id: 'mq', 
      label: 'Message Queue', 
      icon: '📨',
      description: 'ActiveMQ message broker'
    },
    { 
      id: 'risk-engine', 
      label: 'Risk Engine', 
      icon: '⚙️',
      description: 'Risk analysis and scoring'
    },
    { 
      id: 'alert-service', 
      label: 'Alert Service', 
      icon: '🚨',
      description: 'Alert generation and notification'
    }
  ];

  // Simulate system activity for demonstration
  useEffect(() => {
    // Start a periodic animation to show system activity
    const animationInterval = setInterval(() => {
      setIsAnimating(true);
      // Set a random active stage to simulate processing
      const randomIndex = Math.floor(Math.random() * stages.length);
      setActiveStage(stages[randomIndex].id);
      
      // Reset after a short delay
      setTimeout(() => {
        setIsAnimating(false);
        setActiveStage(null);
      }, 1500);
    }, 4000); // Change every 4 seconds
    
    return () => clearInterval(animationInterval);
  }, []);

  // Manual trigger for demonstration (could be connected to real metrics)
  const highlightStage = (stageId) => {
    setActiveStage(stageId);
    setIsAnimating(true);
    setTimeout(() => {
      setIsAnimating(false);
      setActiveStage(null);
    }, 2000);
  };

  return (
    <div className="system-flow-container">
      <div className="system-flow-header">
        <h2>🔄 System Flow Visualization</h2>
        <p className="system-flow-description">
          Visualizing data flow through the transaction monitoring system
        </p>
        <div className="flow-controls">
          <button 
            onClick={() => highlightStage('risk-engine')}
            className="btn btn--small btn--secondary"
          >
            Simulate Risk Analysis
          </button>
          <button 
            onClick={() => highlightStage('alert-service')}
            className="btn btn--small btn--secondary"
          >
            Simulate Alert Generation
          </button>
        </div>
      </div>

      <div className="system-flow-diagram">
        {stages.map((stage, index) => (
          <div key={stage.id} className="flow-stage">
            {/* Stage Icon */}
            <div className={`flow-stage-icon ${activeStage === stage.id ? 'active' : ''} ${isAnimating && activeStage === stage.id ? 'animating' : ''}`}>
              {stage.icon}
            </div>
            
            {/* Stage Label */}
            <div className="flow-stage-label">
              <div className="flow-stage-name">{stage.label}</div>
              <div className="flow-stage-description">{stage.description}</div>
            </div>
            
            {/* Connection Arrow (except for last stage) */}
            {index < stages.length - 1 && (
              <div className="flow-connector">
                <div className={`flow-arrow ${activeStage === stage.id ? 'active' : ''}`}>
                  →
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
      
      {/* Status Indicator */}
      {activeStage && (
        <div className="system-flow-status">
          <span className="status-dot"></span>
          <span className="status-text">
            Active: {stages.find(s => s.id === activeStage)?.label || 'System'}
            {isAnimating ? ' (processing...)' : ''}
          </span>
        </div>
      )}
      
      {/* Legend */}
      <div className="flow-legend">
        <div className="legend-title">Legend:</div>
        <div className="legend-items">
          <div className="legend-item">
            <div className="legend-dot active"></div>
            <span>Active Stage</span>
          </div>
          <div className="legend-item">
            <div className="legend-dot animating"></div>
            <span>Processing</span>
          </div>
          <div className="legend-item">
            <div className="legend-dot"></div>
            <span>Idle / Ready</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SystemFlowVisualization;