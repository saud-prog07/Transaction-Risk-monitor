import React, { useState } from 'react';
import '../styles/FilterBar.css';

/**
 * FilterBar Component - Search and filter controls
 * @param {Object} props
 * @param {function} props.onFilterChange - Callback for filter changes
 * @param {function} props.onSearchChange - Callback for search changes
 */
const FilterBar = ({ onFilterChange, onSearchChange }) => {
  const [search, setSearch] = useState('');
  const [riskLevel, setRiskLevel] = useState('');
  const [status, setStatus] = useState('');

  const handleSearchChange = (e) => {
    const value = e.target.value;
    setSearch(value);
    onSearchChange?.(value);
  };

  const handleRiskLevelChange = (e) => {
    const value = e.target.value;
    setRiskLevel(value);
    onFilterChange?.({ riskLevel: value, status });
  };

  const handleStatusChange = (e) => {
    const value = e.target.value;
    setStatus(value);
    onFilterChange?.({ riskLevel, status: value });
  };

  const handleReset = () => {
    setSearch('');
    setRiskLevel('');
    setStatus('');
    onSearchChange?.('');
    onFilterChange?.({ riskLevel: '', status: '' });
  };

  return (
    <div className="filter-bar">
      <div className="filter-bar__group">
        <input
          type="text"
          className="filter-bar__search"
          placeholder="Search by User ID or Transaction ID..."
          value={search}
          onChange={handleSearchChange}
        />
      </div>

      <div className="filter-bar__group">
        <select
          className="filter-bar__select"
          value={riskLevel}
          onChange={handleRiskLevelChange}
        >
          <option value="">All Risk Levels</option>
          <option value="LOW">Low Risk</option>
          <option value="MEDIUM">Medium Risk</option>
          <option value="HIGH">High Risk</option>
        </select>
      </div>

      <div className="filter-bar__group">
        <select
          className="filter-bar__select"
          value={status}
          onChange={handleStatusChange}
        >
          <option value="">All Statuses</option>
          <option value="NEW">New</option>
          <option value="REVIEWED">Reviewed</option>
          <option value="RESOLVED">Resolved</option>
        </select>
      </div>

      <button className="filter-bar__reset" onClick={handleReset}>
        Reset
      </button>
    </div>
  );
};

export default FilterBar;
