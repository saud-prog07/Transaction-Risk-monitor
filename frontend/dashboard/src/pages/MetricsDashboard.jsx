import React, { useState, useEffect } from 'react';
import axios from 'axios';
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from '@chakra-ui/react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer 
} from 'recharts';
import { useRefresh } from '../hooks';

/**
 * Metrics Dashboard Page
 * Displays system metrics with auto-refresh and charts
 */
export default function MetricsDashboard() {
  const [metrics, setMetrics] = useState({
    totalProcessed: 0,
    flaggedCount: 0,
    failedCount: 0,
    avgProcessingTime: 0,
    throughput: 0,
  });
  
  const [throughputHistory, setThroughputHistory] = useState([]);
  const [successRateHistory, setSuccessRateHistory] = useState([]);
  const [lastUpdated, setLastUpdated] = useState(null);
  
  const { refresh } = useRefresh(5000); // Refresh every 5 seconds

  // Fetch metrics from API
  const fetchMetrics = async () => {
    try {
      const response = await axios.get('/api/metrics');
      const data = response.data;
      setMetrics(data);
      setLastUpdated(new Date());
      
      // Update history charts
      const timestamp = new Date().toLocaleTimeString();
      setThroughputHistory(prev => {
        const newEntry = { time: timestamp, value: data.throughput };
        const updated = [...newEntry, ...prev.slice(0, 29)]; // Keep last 30 points
        return updated;
      });
      
      const successRate = data.totalProcessed > 0 
        ? ((data.totalProcessed - data.failedCount) / data.totalProcessed) * 100 
        : 100;
      setSuccessRateHistory(prev => {
        const newEntry = { time: timestamp, value: successRate };
        const updated = [...newEntry, ...prev.slice(0, 29)]; // Keep last 30 points
        return updated;
      });
    } catch (error) {
      console.error('Failed to fetch metrics:', error);
    }
  };

  // Fetch metrics on mount and refresh
  useEffect(() => {
    fetchMetrics();
  }, [refresh]);

  return (
    <div className="metrics-dashboard p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-100 mb-4">
          System Metrics Dashboard
        </h1>
        <p className="text-gray-400 text-sm">
          Last updated: {lastUpdated ? lastUpdated.toLocaleTimeString() : 'Never'}
        </p>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {/* Total Processed */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Total Transactions Processed
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-100">
              {metrics.totalProcessed.toLocaleString()}
            </div>
            <p className="text-gray-400 mt-2 text-sm">
              Total transactions processed since startup
            </p>
          </CardContent>
        </Card>

        {/* Flagged Transactions */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Flagged Transactions
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-yellow-400">
              {metrics.flaggedCount.toLocaleString()}
            </div>
            <p className="text-gray-400 mt-2 text-sm">
              Transactions flagged for review
            </p>
          </CardContent>
        </Card>

        {/* Failed Messages (DLQ) */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Failed Messages (DLQ)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-red-400">
              {metrics.failedCount.toLocaleString()}
            </div>
            <p className="text-gray-400 mt-2 text-sm">
              Messages sent to Dead Letter Queue
            </p>
          </CardContent>
        </Card>

        {/* Avg Processing Time */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Avg Processing Time
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-400">
              {metrics.avgProcessingTime.toFixed(2)} ms
            </div>
            <p className="text-gray-400 mt-2 text-sm">
              Average time to process each transaction
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Throughput Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Throughput (Transactions/Second)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart
                data={throughputHistory}
                margin={{ top: 20, right: 30, left: 0, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 10 }} />
                <Tooltip />
                <Legend verticalAlign="top" height={36} />
                <Line 
                  type="monotone" 
                  dataKey="value" 
                  stroke="#3b82f6" 
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Success vs Failure Rate Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="text-gray-400 font-medium">
              Success vs Failure Rate (%)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart
                data={successRateHistory}
                margin={{ top: 20, right: 30, left: 0, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" tick={{ fontSize: 10 }} />
                <YAxis 
                  domain={[0, 100]}
                  tick={{ fontSize: 10 }} 
                />
                <Tooltip />
                <Legend verticalAlign="top" height={36} />
                <Line 
                  type="monotone" 
                  dataKey="value" 
                  stroke="#10b981" 
                  strokeWidth={2}
                  dot={false}
                />
                {/* Failure rate line (100 - success rate) */}
                <Line 
                  type="monotone" 
                  dataKey="value" 
                  stroke="#ef4444" 
                  strokeWidth={2}
                  dot={false}
                  // Calculate failure rate by transforming data
                  // We'll handle this in the data preparation instead
                />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}