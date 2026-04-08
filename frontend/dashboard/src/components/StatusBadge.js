import React from 'react';
import { getStatusColor } from '../services/utils';
import '../styles/StatusBadge.css';

/**
 * StatusBadge Component - Alert status indicator
 * @param {Object} props
 * @param {string} props.status - Status (NEW, REVIEWED, RESOLVED)
 * @param {function} props.onClick - Optional click handler
 */
const StatusBadge = ({ status, onClick }) => {
  const colors = getStatusColor(status);

  return (
    <span
      className="status-badge"
      style={{
        backgroundColor: colors.background,
        color: colors.text,
      }}
      onClick={onClick}
      role="button"
      tabIndex="0"
    >
      {status}
    </span>
  );
};

export default StatusBadge;
