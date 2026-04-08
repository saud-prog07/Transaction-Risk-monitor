import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useRefresh } from '../hooks';

/**
 * Dead Letter Queue Dashboard
 * Displays failed messages with retry and resolution capabilities
 */
export default function DLQDashboard() {
  const [dlqMessages, setDlqMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    status: 'PENDING', // PENDING, RETRYING, RESOLVED, DEAD
    page: 0,
    size: 50
  });
  const [stats, setStats] = useState({
    totalMessages: 0,
    pendingMessages: 0,
    retryingMessages: 0,
    resolvedMessages: 0,
    deadMessages: 0
  });
  
  const { refresh } = useRefresh(5000); // Refresh every 5 seconds

  // Fetch DLQ statistics
  const fetchStatistics = async () => {
    try {
      const response = await axios.get('/api/dlq/statistics');
      setStats(response.data);
    } catch (err) {
      console.error('Failed to fetch DLQ statistics:', err);
    }
  };

  // Fetch DLQ messages
  const fetchDlqMessages = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await axios.get('/api/dlq/messages', {
        params: {
          status: filters.status,
          page: filters.page,
          size: filters.size
        }
      });
      
      setDlqMessages(response.data.content || []);
      setLoading(false);
    } catch (err) {
      console.error('Failed to fetch DLQ messages:', err);
      setError('Failed to load DLQ messages');
      setLoading(false);
    }
  };

  // Retry a specific DLQ message
  const retryMessage = async (dlqId) => {
    try {
      await axios.post(`/api/dlq/${dlqId}/retry`);
      // Refresh the message list after retry
      await fetchDlqMessages();
      await fetchStatistics();
    } catch (err) {
      console.error('Failed to retry message:', err);
      setError('Failed to retry message');
    }
  };

  // Mark message as resolved (skip retries)
  const markAsResolved = async (dlqId, notes = '') => {
    try {
      await axios.post(`/api/dlq/${dlqId}/skip-retries`, { notes });
      // Refresh the message list after resolving
      await fetchDlqMessages();
      await fetchStatistics();
    } catch (err) {
      console.error('Failed to mark message as resolved:', err);
      setError('Failed to mark message as resolved');
    }
  };

  // Fetch data on mount and refresh
  useEffect(() => {
    fetchStatistics();
    fetchDlqMessages();
  }, [refresh]);

  return (
    <div className="dlq-dashboard p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-100 mb-4">
          Dead Letter Queue Dashboard
        </h1>
        <p className="text-gray-400 text-sm">
          Last updated: {new Date().toLocaleTimeString()}
        </p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
        {/* Total Messages */}
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <h3 className="text-sm font-medium text-gray-400">Total Messages</h3>
          <p className="text-2xl font-bold text-gray-100">{stats.totalMessages}</p>
        </div>
        
        {/* Pending Messages */}
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <h3 className="text-sm font-medium text-gray-400">Pending</h3>
          <p className="text-2xl font-bold text-yellow-400">{stats.pendingMessages}</p>
        </div>
        
        {/* Retrying Messages */}
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <h3 className="text-sm font-medium text-gray-400">Retrying</h3>
          <p className="text-2xl font-bold text-blue-400">{stats.retryingMessages}</p>
        </div>
        
        {/* Resolved Messages */}
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <h3 className="text-sm font-medium text-gray-400">Resolved</h3>
          <p className="text-2xl font-bold text-green-400">{stats.resolvedMessages}</p>
        </div>
        
        {/* Dead Messages */}
        <div className="bg-gray-800 rounded-lg p-4 border border-gray-700">
          <h3 className="text-sm font-medium text-gray-400">Dead (Max Retries)</h3>
          <p className="text-2xl font-bold text-red-400">{stats.deadMessages}</p>
        </div>
      </div>

      {/* Controls and Filters */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6">
        <div className="flex flex-col md:flex-row md:items-center mb-4 md:mb-0">
          <label className="text-gray-400 mb-2 md:mb-0 md:mr-4">Filter by Status:</label>
          <select
            value={filters.status}
            onChange={(e) => {
              setFilters(prev => ({ ...prev, status: e.target.value, page: 0 }));
              fetchDlqMessages();
            }}
            className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="PENDING">Pending</option>
            <option value="RETRYING">Retrying</option>
            <option value="RESOLVED">Resolved</option>
            <option value="DEAD">Dead</option>
          </select>
        </div>
        
        {loading && (
          <div className="flex items-center">
            <div className="animate-spin rounded-full border-4 border-b-blue-500 h-8 w-8"></div>
            <span className="ml-2 text-gray-400">Loading messages...</span>
          </div>
        )}
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-900/20 border border-red-500/50 text-red-400 px-4 py-3 rounded-lg mb-6">
          {error}
        </div>
      )}

      {/* Messages Table */}
      <div className="overflow-x-auto">
        {dlqMessages.length === 0 && !loading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">
              {filters.status === 'PENDING' 
                ? 'No pending messages in DLQ' 
                : `No ${filters.status.toLowerCase()} messages in DLQ`}
            </p>
            <p className="text-gray-600 text-sm mt-2">
              Messages will appear here as they fail processing
            </p>
          </div>
        ) : (
          <table className="min-w-full divide-y divide-gray-700">
            <thead className="bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                  Transaction ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                  Error Message
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                  Retry Count
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-400 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-gray-800 divide-y divide-gray-700">
              {dlqMessages.map((msg) => (
                <tr key={msg.id} className="hover:bg-gray-700/50 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-100">
                    {msg.transactionId || 'Unknown'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300 break-words">
                    {msg.errorMessage || 'No error message'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-100">
                    {msg.retryCount || 0}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <span 
                      className={`px-2 py-1 rounded-full text-xs font-medium 
                        ${msg.status === 'PENDING' ? 'bg-yellow-900/20 text-yellow-400' : ''}
                        ${msg.status === 'RETRYING' ? 'bg-blue-900/20 text-blue-400' : ''}
                        ${msg.status === 'RESOLVED' ? 'bg-green-900/20 text-green-400' : ''}
                        ${msg.status === 'DEAD' ? 'bg-red-900/20 text-red-400' : ''}
                      `}
                    >
                      {msg.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm flex space-x-2">
                    {/* Retry Button */}
                    {msg.status !== 'RESOLVED' && msg.status !== 'DEAD' && (
                      <button
                        onClick={() => retryMessage(msg.id)}
                        disabled={loading}
                        className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded 
                          text-sm transition-colors disabled:opacity-50"
                      >
                        Retry
                      </button>
                    )}
                    
                    {/* Mark as Resolved Button */}
                    {msg.status !== 'RESOLVED' && msg.status !== 'DEAD' && (
                      <button
                        onClick={() => {
                          const notes = window.prompt('Enter resolution notes (optional):', '');
                          markAsResolved(msg.id, notes || '');
                        }}
                        disabled={loading}
                        className="px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white font-medium rounded 
                          text-sm transition-colors disabled:opacity-50"
                      >
                        Resolve
                      </button>
                    )}
                    
                    {/* Force Delete Button (for DEAD messages) */}
                    {msg.status === 'DEAD' && (
                      <button
                        onClick={() => {
                          if (window.confirm('Are you sure you want to permanently delete this message?')) {
                            // In a real implementation, this would call a delete endpoint
                            // For now, we'll just remove it from the list
                            setDlqMessages(prev => prev.filter(m => m.id !== msg.id));
                          }
                        }}
                        disabled={loading}
                        className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white font-medium rounded 
                          text-sm transition-colors disabled:opacity-50"
                      >
                        Delete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      
      {/* Pagination Controls */}
      {dlqMessages.length > 0 && (
        <div className="mt-6 flex flex-col md:flex-row md:items-center md:justify-between">
          <div className="text-gray-400 text-sm">
            Showing {dlqMessages.length} messages
          </div>
          <div className="flex space-x-2">
            <button
              onClick={() => {
                setFilters(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }));
                fetchDlqMessages();
              }}
              disabled={filters.page === 0 || loading}
              className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white font-medium rounded 
                transition-colors disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => {
                setFilters(prev => ({ ...prev, page: prev.page + 1 }));
                fetchDlqMessages();
              }}
              disabled={loading}
              className="px-3 py-1.5 bg-gray-700 hover:bg-gray-600 text-white font-medium rounded 
                transition-colors disabled:opacity-50"
            >
              Next
            </button>
          </div>
        )
      )}
    </div>
  );
}