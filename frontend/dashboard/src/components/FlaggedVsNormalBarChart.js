import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

/**
 * FlaggedVsNormalBarChart Component
 * Displays comparison between flagged and normal transactions
 * @param {Array} data - Array of data points with category and count
 */
const FlaggedVsNormalBarChart = ({ data }) => {
  const chartData = [
    {
      name: 'Transaction Status',
      'Flagged': data[0]?.count || 0,
      'Normal': data[1]?.count || 0,
    },
  ];

  // Custom tooltip to show percentage
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const data = payload[0];
      return (
        <div style={{
          backgroundColor: '#ffffff',
          border: '1px solid #e5e7eb',
          borderRadius: '8px',
          padding: '8px 12px',
          boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        }}>
          <p style={{ margin: 0, fontWeight: 600, color: '#1f2937' }}>
            {data.name}
          </p>
          <p style={{ margin: '4px 0 0 0', color: data.fill }}>
            Count: {data.value}
          </p>
        </div>
      );
    }
    return null;
  };

  // Calculate percentages
  const total = (chartData[0]['Flagged'] || 0) + (chartData[0]['Normal'] || 0);
  const flaggedPercent = total > 0 ? ((chartData[0]['Flagged'] / total) * 100).toFixed(1) : 0;
  const normalPercent = total > 0 ? ((chartData[0]['Normal'] / total) * 100).toFixed(1) : 0;

  chartData[0].flaggedPercent = flaggedPercent;
  chartData[0].normalPercent = normalPercent;

  return (
    <div className="chart-container">
      <h3 className="chart-title">
        Flagged vs Normal Transactions
        <span className="chart-subtitle">
          Flagged: {flaggedPercent}% | Normal: {normalPercent}%
        </span>
      </h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={chartData} margin={{ top: 5, right: 30, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis
            dataKey="name"
            tick={{ fontSize: 12, fill: '#6b7280' }}
          />
          <YAxis tick={{ fontSize: 12, fill: '#6b7280' }} />
          <Tooltip content={<CustomTooltip />} />
          <Legend wrapperStyle={{ paddingTop: '20px' }} />
          <Bar dataKey="Flagged" fill="#ef4444" radius={[8, 8, 0, 0]} />
          <Bar dataKey="Normal" fill="#10b981" radius={[8, 8, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default FlaggedVsNormalBarChart;
