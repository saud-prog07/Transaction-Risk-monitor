import React from 'react';
import { getRiskLevelColor } from '../services/utils';
import '../styles/RiskBadge.css';

/**
 * RiskBadge Component - Color-coded risk level indicator
 * @param {Object} props
 * @param {string} props.level - Risk level (LOW, MEDIUM, HIGH)
 * @param {number} props.score - Optional risk score to display
 */
const RiskBadge = ({ level, score }) => {
  const colors = getRiskLevelColor(level);

  return (
    <span
      className="risk-badge"
      style={{
        backgroundColor: colors.background,
        color: colors.text,
        borderColor: colors.border,
      }}
    >
      {level}
      {score !== undefined && ` (${score.toFixed(1)})`}
    </span>
  );
};

export default RiskBadge;
