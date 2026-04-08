import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';

/**
 * RiskLevelChart Component
 * Displays breakdown of transactions by risk level
 * @param {Array} data - Array of data with risk level and count
 */
const RiskLevelChart = ({ data }) => {
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      const totalValue = data.reduce((sum, d) => sum + d.value, 0);
      const itemPercentage = totalValue > 0 ? ((item.value / totalValue) * 100).toFixed(1) : 0;
      return (
        <div style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: '8px 12px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        }}>
          <p style={{ margin: 0, fontWeight: 600, color: '#1f2937' }}>
            {item.name}
          </p>
          <p style={{ margin: '4px 0 0 0', color: item.fill }}>
            Count: {item.value}
          </p>
          <p style={{ margin: '2px 0 0 0', color: '#6b7280', fontSize: '12px' }}>
            {itemPercentage}% of total
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="chart-container">
      <h3 className="chart-title">Risk Level Breakdown</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart
          data={data}
          layout="vertical"
          margin={{ top: 5, right: 30, left: 100, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis type="number" tick={{ fontSize: 12, fill: '#6b7280' }} />
          <YAxis
            dataKey="name"
            type="category"
            tick={{ fontSize: 12, fill: '#6b7280' }}
            width={95}
          />
          <Tooltip content={<CustomTooltip />} />
          <Bar dataKey="value" fill="#3b82f6" radius={[0, 8, 8, 0]}>
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default RiskLevelChart;
