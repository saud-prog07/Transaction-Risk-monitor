import axios from 'axios';
import { getErrorMessage } from './apiService';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8082';

/**
 * Configuration Service - Frontend
 * Handles API calls for system configuration management
 */

/**
 * Fetch current system configuration
 * @returns {Promise} Configuration data
 */
export const fetchConfiguration = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/api/config`);
    return response.data;
  } catch (error) {
    const errorMessage = getErrorMessage(error);
    throw new Error(errorMessage);
  }
};

/**
 * Update system configuration
 * @param {Object} configData - Configuration object with settings to update
 * @returns {Promise} Updated configuration response
 */
export const updateConfiguration = async (configData) => {
  try {
    const response = await axios.put(`${API_BASE_URL}/api/config`, configData);
    return response.data;
  } catch (error) {
    const errorMessage = getErrorMessage(error);
    throw new Error(errorMessage);
  }
};

/**
 * Get default configuration values
 * @returns {Object} Default configuration
 */
export const getDefaultConfiguration = () => {
  return {
    highRiskThreshold: 80,
    mediumRiskThreshold: 50,
    lowRiskThreshold: 20,
    anomalyMultiplier: 1.5,
    velocityCheckEnabled: true,
    velocityThreshold: 10,
    geolocationCheckEnabled: true,
    amountSpikeCheckEnabled: true,
    amountSpikeMultiplier: 2.0,
    enforceMfaForHighRisk: true,
    autoEscalationEnabled: true,
  };
};
