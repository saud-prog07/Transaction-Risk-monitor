import React from 'react';
import '../styles/StatCard.css';

/**
 * StatCard Component - Displays a single statistic
 * @param {Object} props
 * @param {string} props.title - Card title
 * @param {string|number} props.value - Stat value
 * @param {string} props.icon - Icon emoji or symbol
 * @param {string} props.color - Color theme (primary, success, warning, danger)
 * @param {string} props.subtitle - Optional subtitle text
 */
const StatCard = ({ title, value, icon, color = 'primary', subtitle }) => {
  return (
    <div className={`stat-card stat-card--${color}`}>
      <div className="stat-card__icon">{icon}</div>
      <div className="stat-card__content">
        <h3 className="stat-card__title">{title}</h3>
        <p className="stat-card__value">{value}</p>
        {subtitle && <p className="stat-card__subtitle">{subtitle}</p>}
      </div>
    </div>
  );
};

export default StatCard;
