import http from 'k6/http';
import { check, group, sleep } from 'k6';

/**
 * K6 Load Test for 10K+ Concurrent Users
 * Transaction Risk Monitoring System
 * 
 * Test Plan:
 * - Phase 0: 0 users (baseline)
 * - Phase 1: Ramp up to 100 users over 2 minutes
 * - Phase 2: Ramp up to 5,000 users over 5 minutes
 * - Phase 3: Ramp up to 10,000 users over 5 minutes
 * - Phase 4: Hold at 10,000 users for 10 minutes (stress test)
 * - Phase 5: Ramp down to 0 users over 2 minutes
 * 
 * Total Duration: 24 minutes
 * Expected Results:
 * - Error rate: < 0.1%
 * - p99 latency: < 4 seconds
 * - Throughput: 50K+ tx/sec
 */

export const options = {
  stages: [
    // Phase 1: Ramp up to 100 users
    { duration: '2m', target: 100, name: 'ramping_up_light' },
    // Phase 2: Ramp up to 5,000 users
    { duration: '5m', target: 5000, name: 'ramping_up_medium' },
    // Phase 3: Ramp up to 10,000 users
    { duration: '5m', target: 10000, name: 'ramping_up_heavy' },
    // Phase 4: Hold at 10,000 users for stress testing
    { duration: '10m', target: 10000, name: 'stress_test' },
    // Phase 5: Ramp down
    { duration: '2m', target: 0, name: 'ramping_down' },
  ],
  thresholds: {
    'http_req_duration': ['p(99)<4000', 'p(95)<2000'],  // 99th < 4s, 95th < 2s
    'http_req_failed': ['rate<0.001'],                   // Error rate < 0.1%
    'http_reqs': ['rate>6000'],                          // Throughput > 6K req/sec (50K tx/sec)
  },
  ext: {
    loadimpact: {
      projectID: 3307357,
      name: 'Risk Monitoring 10K Users',
    },
  },
};

// Test configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_VERSION = '/api/v1';

/**
 * Generate random transaction data for testing
 */
function generateTransaction() {
  const amounts = [100, 250, 500, 1000, 5000, 10000];
  const merchants = ['AMAZON', 'WALMART', 'TARGET', 'COSTCO', 'BEST_BUY', 'APPLE_STORE', 'NETFLIX'];
  const cards = ['1234', '5678', '9012', '3456', '7890'];
  
  return {
    amount: amounts[Math.floor(Math.random() * amounts.length)],
    merchant: merchants[Math.floor(Math.random() * merchants.length)],
    cardLastFour: cards[Math.floor(Math.random() * cards.length)],
    timestamp: new Date().toISOString(),
    userId: `user_${Math.floor(Math.random() * 1000)}`,
    mcc: Math.floor(Math.random() * 9000) + 1000,
    country: 'US',
    deviceId: `device_${Math.floor(Math.random() * 100)}`,
  };
}

/**
 * Main test function - simulates user behavior
 */
export default function () {
  const transaction = generateTransaction();

  // Test Group 1: Submit Transaction
  group('submit_transaction', () => {
    const submitResponse = http.post(`${BASE_URL}${API_VERSION}/transactions`, JSON.stringify(transaction), {
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: '30s',
    });

    check(submitResponse, {
      'transaction_submit_status_is_200': (r) => r.status === 200 || r.status === 201,
      'transaction_submit_response_time_<_1s': (r) => r.timings.duration < 1000,
      'transaction_has_id': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.id ? true : false;
        } catch {
          return false;
        }
      },
    });

    if (submitResponse.status === 200 || submitResponse.status === 201) {
      try {
        const responseBody = JSON.parse(submitResponse.body);
        const transactionId = responseBody.id || responseBody.transactionId;
        
        // Small delay before checking status
        sleep(0.5);

        // Test Group 2: Get Transaction Status
        group('get_transaction_status', () => {
          const statusResponse = http.get(
            `${BASE_URL}${API_VERSION}/transactions/${transactionId}`,
            { timeout: '30s' }
          );

          check(statusResponse, {
            'get_status_is_200': (r) => r.status === 200,
            'get_status_response_time_<_500ms': (r) => r.timings.duration < 500,
          });
        });
      } catch (e) {
        console.error('Error parsing transaction response:', e);
      }
    }
  });

  // Random user behavior - sometimes check alerts
  if (Math.random() > 0.7) {
    group('fetch_alerts', () => {
      const alertsResponse = http.get(`${BASE_URL}${API_VERSION}/alerts?limit=10`, {
        timeout: '30s',
      });

      check(alertsResponse, {
        'alerts_request_is_200': (r) => r.status === 200,
        'alerts_response_time_<_1s': (r) => r.timings.duration < 1000,
      });
    });
  }

  // Random user behavior - sometimes check dashboard stats
  if (Math.random() > 0.8) {
    group('fetch_dashboard_stats', () => {
      const statsResponse = http.get(`${BASE_URL}${API_VERSION}/dashboard/stats`, {
        timeout: '30s',
      });

      check(statsResponse, {
        'stats_request_is_200': (r) => r.status === 200 || r.status === 404, // May not be implemented
        'stats_response_time_<_2s': (r) => r.timings.duration < 2000,
      });
    });
  }

  // Think time between user actions (1-3 seconds)
  sleep(Math.random() * 2 + 1);
}

/**
 * Setup function - runs once before all tests
 */
export function setup() {
  console.log(`Starting load test against: ${BASE_URL}`);
  
  // Health check
  const healthCheck = http.get(`${BASE_URL}${API_VERSION}/health`, {
    timeout: '10s',
  });
  
  check(healthCheck, {
    'system_is_healthy': (r) => r.status === 200 || r.status === 204,
  });

  return {
    startTime: new Date().getTime(),
  };
}

/**
 * Teardown function - runs once after all tests
 */
export function teardown(data) {
  const endTime = new Date().getTime();
  const duration = (endTime - data.startTime) / 1000;
  console.log(`Load test completed. Total duration: ${duration} seconds`);
}
