import React from 'react';
import '../styles/BusinessImpactChart.css';

/**
 * BusinessImpactChart Component
 * Simple chart component for displaying business metrics trends
 */
const BusinessImpactChart = ({ data, title, subtitle, color, yLabel }) => {
  if (!data || data.length === 0) {
    return (
      <div className="business-chart-container">
        <h3 className="chart-title">{title}</h3>
        <p className="chart-subtitle">{subtitle}</p>
        <div className="chart-placeholder">
          <div className="chart-empty">No data available</div>
        </div>
      </div>
    );
  }

  // Find min and max values for scaling
  const values = data.map(d => d.y);
  const minY = Math.min(...values);
  const maxY = Math.max(...values);
  const yRange = maxY - minY || 1; // Avoid division by zero

  return (
    <div className="business-chart-container">
      <h3 className="chart-title">{title}</h3>
      <p className="chart-subtitle">{subtitle}</p>
      <div className="chart-placeholder">
        <svg className="business-chart-svg" viewBox="0 0 100 60" preserveAspectRatio="xMidYMid meet">
          {/* Grid lines */}
          {[0, 20, 40, 60, 80, 100].map(y => (
            <line key={`h-${y}`} x1="0" y1={y} x2="100" y2={y} stroke="#374151" strokeWidth="0.5" />
          ))}
          {[0, 20, 40, 60, 80, 100].map(x => (
            <line key={`v-${x}`} x1={x} y1="0" x2={x} y2="60" stroke="#374151" strokeWidth="0.5" />
          ))}

          {/* Chart line */}
          <path 
            d={`M 0 ${100 - ((data[0].y - minY) / yRange * 100)} 
               ${data.map((d, i) => 
                 `${i * (100 / (data.length - 1 || 1))} ${100 - ((d.y - minY) / yRange * 100)}`
               ).join(' ')}`}
            fill="none"
            stroke={color}
            strokeWidth="2"
          />

          {/* Data points */}
          {data.map((d, i) => (
            <circle 
              key={i} 
              cx={i * (100 / (data.length - 1 || 1))} 
              cy={100 - ((d.y - minY) / yRange * 100)} 
              r="3" 
              fill={color} 
            />
          ))}

          {/* Y-axis labels */}
          {[0, 25, 50, 75, 100].map(percent => (
            <text key={`y-label-${percent}`} x="-5" y={100 - percent + 3} 
                  font-size="8" fill="#9ca3af" text-anchor="end">
              ${((minY + (percent / 100) * yRange)).toFixed(yLabel === 'currency' ? 2 : 2)}
            </text>
          ))}

          {/* X-axis labels (dates) */}
          {data.map((d, i) => (
            <text key={`x-label-${i}`} 
                  x={i * (100 / (data.length - 1 || 1))} 
                  y="110" 
                  font-size="8" 
                  fill="#9ca3af" 
                  text-anchor="middle"
                  transform={`rotate(-45 ${i * (100 / (data.length - 1 || 1))},110`}>
              ${d.x}
            </text>
          ))}
        </svg>
      </div>
    </div>
  );
};

export default BusinessImpactChart;