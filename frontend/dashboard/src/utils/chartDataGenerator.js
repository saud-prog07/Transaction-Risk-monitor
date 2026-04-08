/**
 * Chart Data Generator Utility
 * Aggregates alert data for real-time chart visualization
 */

/**
 * Generate time-series data for transaction volume chart
 * Groups transactions by 5-minute intervals over the last hour
 * @param {Array} alerts - Array of alert objects
 * @returns {Array} Array of data points with timestamp and transaction counts
 */
export const generateTransactionTimeSeriesData = (alerts) => {
  const now = new Date();
  const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

  // Initialize hourly buckets (12 intervals of 5 minutes)
  const timeSlots = [];
  for (let i = 11; i >= 0; i--) {
    const slotTime = new Date(now.getTime() - i * 5 * 60 * 1000);
    timeSlots.push({
      time: slotTime.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      timestamp: slotTime.getTime(),
      total: 0,
      flagged: 0,
    });
  }

  // Count transactions in each time slot
  alerts.forEach((alert) => {
    const alertTime = new Date(alert.createdAt).getTime();

    if (alertTime >= oneHourAgo.getTime()) {
      // Find the appropriate 5-minute slot
      const slotIndex = timeSlots.findIndex((slot) => {
        const slotStart = slot.timestamp;
        const slotEnd = slot.timestamp + 5 * 60 * 1000;
        return alertTime >= slotStart && alertTime < slotEnd;
      });

      if (slotIndex !== -1) {
        timeSlots[slotIndex].total++;
        if (alert.riskLevel === 'HIGH' || alert.riskLevel === 'MEDIUM') {
          timeSlots[slotIndex].flagged++;
        }
      }
    }
  });

  return timeSlots;
};

/**
 * Generate data for flagged vs normal transactions bar chart
 * @param {Array} alerts - Array of alert objects
 * @returns {Array} Array with risk level breakdown
 */
export const generateRiskLevelChartData = (alerts) => {
  const riskCounts = {
    HIGH: alerts.filter((a) => a.riskLevel === 'HIGH').length,
    MEDIUM: alerts.filter((a) => a.riskLevel === 'MEDIUM').length,
    LOW: alerts.filter((a) => a.riskLevel === 'LOW').length,
  };

  return [
    {
      name: 'High Risk',
      value: riskCounts.HIGH,
      fill: '#ef4444',
      risk: 'HIGH',
    },
    {
      name: 'Medium Risk',
      value: riskCounts.MEDIUM,
      fill: '#f59e0b',
      risk: 'MEDIUM',
    },
    {
      name: 'Low Risk',
      value: riskCounts.LOW,
      fill: '#10b981',
      risk: 'LOW',
    },
  ];
};

/**
 * Generate data for flagged vs normal transactions comparison
 * @param {Array} alerts - Array of alert objects
 * @returns {Array} Array with flagged and normal transaction counts
 */
export const generateFlaggedVsNormalData = (alerts) => {
  const flagged = alerts.filter(
    (a) => a.riskLevel === 'HIGH' || a.riskLevel === 'MEDIUM'
  ).length;
  const normal = alerts.filter(
    (a) => a.riskLevel === 'LOW'
  ).length;
  const total = alerts.length;

  return [
    {
      category: 'Flagged',
      count: flagged,
      percentage: total > 0 ? ((flagged / total) * 100).toFixed(1) : 0,
      fill: '#ef4444',
    },
    {
      category: 'Normal',
      count: normal,
      percentage: total > 0 ? ((normal / total) * 100).toFixed(1) : 0,
      fill: '#10b981',
    },
  ];
};

/**
 * Generate data for transaction status breakdown
 * @param {Array} alerts - Array of alert objects
 * @returns {Array} Array with status breakdown
 */
export const generateStatusChartData = (alerts) => {
  const statusCounts = {
    NEW: alerts.filter((a) => a.status === 'NEW').length,
    REVIEWED: alerts.filter((a) => a.status === 'REVIEWED').length,
    RESOLVED: alerts.filter((a) => a.status === 'RESOLVED').length,
  };

  return [
    {
      name: 'New',
      value: statusCounts.NEW,
      fill: '#3b82f6',
      status: 'NEW',
    },
    {
      name: 'Reviewed',
      value: statusCounts.REVIEWED,
      fill: '#8b5cf6',
      status: 'REVIEWED',
    },
    {
      name: 'Resolved',
      value: statusCounts.RESOLVED,
      fill: '#10b981',
      status: 'RESOLVED',
    },
  ];
};
