import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  Heading,
  Spinner,
  Alert,
  AlertIcon,
  AlertDescription,
  VStack,
  HStack,
  Text,
  Badge,
  Card,
  CardBody,
  CardHeader,
  Divider,
  Progress
} from '@chakra-ui/react';
import '../styles/TransactionTraceView.css';
import { apiClient } from '../services/apiService';

/**
 * TransactionTraceView Component
 * Displays the lifecycle timeline of a selected transaction with real backend data
 * Shows: Received → Queued → Processed → Flagged → Alerted
 * 
 * Features:
 * - Real-time transaction trace from backend API
 * - Timeline visualization with accurate timestamps
 * - Risk information and analysis results
 * - Processing duration analytics
 */
const TransactionTraceView = ({ transactionId }) => {
  const [traceData, setTraceData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (transactionId) {
      fetchTransactionTrace();
    } else {
      setLoading(false);
      setTraceData(null);
    }
  }, [transactionId]);

  /**
   * Fetches transaction trace from backend API
   * Endpoint: GET /api/transactions/trace/{transactionId}
   * Requires JWT authentication with ANALYST or ADMIN role
   */
  const fetchTransactionTrace = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Call backend API
      const response = await apiClient.get(
        `/api/transactions/trace/${transactionId}`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('access_token')}`
          }
        }
      );
      
      setTraceData(response.data);
      console.log('Transaction trace fetched successfully:', response.data);
    } catch (err) {
      console.error('Error fetching transaction trace:', err);
      
      // Handle specific error cases
      if (err.response?.status === 404) {
        setError('Transaction not found');
      } else if (err.response?.status === 403) {
        setError('You do not have permission to view this transaction');
      } else if (err.response?.status === 401) {
        setError('Authentication required. Please login again.');
      } else {
        setError(err.response?.data?.message || 'Failed to load transaction trace');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Formats timestamp to readable format
   */
  const formatTimestamp = (isoString) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    });
  };

  /**
   * Gets stage icon based on stage name
   */
  const getStageIcon = (stage) => {
    const icons = {
      'RECEIVED': '📥',
      'QUEUED': '⏳',
      'PROCESSED': '🔍',
      'FLAGGED': '🚩',
      'ALERTED': '🚨'
    };
    return icons[stage] || '◎';
  };

  /**
   * Gets stage color based on stage name
   */
  const getStageColor = (stage) => {
    const colors = {
      'RECEIVED': 'blue',
      'QUEUED': 'yellow',
      'PROCESSED': 'green',
      'FLAGGED': 'red',
      'ALERTED': 'orange'
    };
    return colors[stage] || 'gray';
  };

  /**
   * Gets risk level color
   */
  const getRiskLevelColor = (riskLevel) => {
    const colors = {
      'HIGH': 'red',
      'MEDIUM': 'orange',
      'LOW': 'green'
    };
    return colors[riskLevel] || 'gray';
  };

  if (loading) {
    return (
      <Box className="trace-view-container" p={6} bg="white" borderRadius="md" boxShadow="sm">
        <VStack spacing={4} align="center" justify="center" minH="400px">
          <Spinner size="lg" color="blue.500" thickness="4px" />
          <Text fontSize="lg" color="gray.600">Loading transaction trace...</Text>
        </VStack>
      </Box>
    );
  }

  if (error) {
    return (
      <Box className="trace-view-container" p={6} bg="white" borderRadius="md" boxShadow="sm">
        <Alert status="error" variant="subtle" flexDirection="column" alignItems="flex-start" borderRadius="md">
          <HStack spacing={3} mb={2}>
            <AlertIcon />
            <Heading size="md">Error Loading Transaction Trace</Heading>
          </HStack>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      </Box>
    );
  }

  if (!traceData) {
    return (
      <Box className="trace-view-container" p={6} bg="white" borderRadius="md" boxShadow="sm">
        <VStack spacing={4} align="center" justify="center" minH="300px">
          <Text fontSize="lg" color="gray.500">Select a transaction to view its processing timeline</Text>
        </VStack>
      </Box>
    );
  }

  return (
    <Container maxW="lg" py={6} px={4} className="trace-view-container">
      <Card bg="white" boxShadow="sm" borderRadius="md">
        <CardHeader pb={4}>
          <VStack align="flex-start" spacing={3} width="100%">
            <Heading size="lg">Transaction Lifecycle Trace</Heading>
            <HStack spacing={4} width="100%" wrap="wrap">
              <Box>
                <Text fontSize="sm" color="gray.600">Transaction ID</Text>
                <Text fontSize="sm" fontFamily="monospace" fontWeight="bold">{traceData.transactionId}</Text>
              </Box>
              <Box>
                <Text fontSize="sm" color="gray.600">Risk Level</Text>
                <Badge colorScheme={getRiskLevelColor(traceData.riskLevel)} fontSize="sm">
                  {traceData.riskLevel}
                </Badge>
              </Box>
              {traceData.riskScore !== null && (
                <Box>
                  <Text fontSize="sm" color="gray.600">Risk Score</Text>
                  <Text fontSize="sm" fontWeight="bold">{traceData.riskScore.toFixed(1)}/100</Text>
                </Box>
              )}
              <Box>
                <Text fontSize="sm" color="gray.600">Status</Text>
                <Badge colorScheme="blue" fontSize="sm">{traceData.alertStatus}</Badge>
              </Box>
            </HStack>
            <Divider />
            {traceData.reason && (
              <Box width="100%">
                <Text fontSize="sm" color="gray.600">Alert Reason</Text>
                <Text fontSize="sm" mt={1}>{traceData.reason}</Text>
              </Box>
            )}
          </VStack>
        </CardHeader>

        <CardBody>
          <VStack spacing={6} align="stretch">
            {/* Timeline */}
            <Box>
              <Heading size="sm" mb={4}>Processing Timeline</Heading>
              
              {traceData.stages && traceData.stages.length > 0 ? (
                <VStack align="stretch" spacing={0}>
                  {traceData.stages.map((stage, index) => (
                    <Box key={index} position="relative" pb={index < traceData.stages.length - 1 ? 6 : 0}>
                      {/* Connecting line (vertical) */}
                      {index < traceData.stages.length - 1 && (
                        <Box
                          position="absolute"
                          left="20px"
                          top="40px"
                          width="2px"
                          height="calc(100% + 24px)"
                          bg="gray.300"
                        />
                      )}
                      
                      {/* Stage item */}
                      <HStack align="flex-start" spacing={4}>
                        {/* Stage indicator */}
                        <Box
                          width="40px"
                          height="40px"
                          borderRadius="50%"
                          bg={`${getStageColor(stage.stage)}.100`}
                          border={`2px solid var(--chakra-colors-${getStageColor(stage.stage)}-500)`}
                          display="flex"
                          alignItems="center"
                          justifyContent="center"
                          flexShrink={0}
                          zIndex={1}
                          position="relative"
                        >
                          <Text fontSize="xl">{getStageIcon(stage.stage)}</Text>
                        </Box>

                        {/* Stage details */}
                        <Box flex={1} pb={4}>
                          <HStack mb={2} justify="space-between">
                            <Badge colorScheme={getStageColor(stage.stage)} fontSize="xs">
                              {stage.stage}
                            </Badge>
                            <Text fontSize="xs" color="gray.500">
                              {formatTimestamp(stage.timestamp)}
                            </Text>
                          </HStack>

                          <VStack align="flex-start" spacing={2}>
                            <Box>
                              <Text fontSize="sm" fontWeight="semibold">{stage.description}</Text>
                              {stage.statusMessage && (
                                <Text fontSize="xs" color="gray.600" mt={1}>{stage.statusMessage}</Text>
                              )}
                            </Box>

                            <HStack spacing={4} fontSize="xs" color="gray.600">
                              {stage.durationFromPreviousMs !== undefined && stage.durationFromPreviousMs > 0 && (
                                <Box>
                                  <Text><strong>Duration:</strong> {stage.durationFromPreviousMs}ms</Text>
                                </Box>
                              )}
                              {stage.cumulativeTimeMs !== undefined && stage.cumulativeTimeMs > 0 && (
                                <Box>
                                  <Text><strong>Cumulative:</strong> {stage.cumulativeTimeMs}ms</Text>
                                </Box>
                              )}
                              {stage.service && (
                                <Badge colorScheme="purple" fontSize="xs">
                                  {stage.service}
                                </Badge>
                              )}
                            </HStack>

                            {stage.additionalData && (
                              <Text fontSize="xs" color="gray.600" fontStyle="italic">
                                {stage.additionalData}
                              </Text>
                            )}
                          </VStack>
                        </Box>
                      </HStack>
                    </Box>
                  ))}
                </VStack>
              ) : (
                <Alert status="warning" variant="subtle" borderRadius="md">
                  <AlertIcon />
                  <Box>
                    <AlertDescription>No timeline stages available</AlertDescription>
                  </Box>
                </Alert>
              )}
            </Box>

            {/* Summary */}
            {traceData.totalProcessingTimeMs !== undefined && (
              <Box bg="gray.50" p={4} borderRadius="md">
                <Heading size="sm" mb={3}>Processing Summary</Heading>
                <VStack align="flex-start" spacing={2} fontSize="sm">
                  <Box width="100%">
                    <HStack justify="space-between" mb={1}>
                      <Text>Total Processing Time:</Text>
                      <Text fontWeight="bold">{traceData.totalProcessingTimeMs}ms</Text>
                    </HStack>
                    <Progress value={(traceData.totalProcessingTimeMs / 10000) * 100} size="sm" colorScheme="blue" />
                  </Box>
                  <HStack color="gray.600" fontSize="xs">
                    <Text>Alert Created:</Text>
                    <Text fontFamily="monospace">{formatTimestamp(traceData.alertCreatedAt)}</Text>
                  </HStack>
                  <HStack color="gray.600" fontSize="xs">
                    <Text>Status:</Text>
                    <Badge colorScheme={traceData.alertStatus === 'NEW' ? 'red' : 'green'} fontSize="xs">
                      {traceData.alertStatus}
                    </Badge>
                  </HStack>
                </VStack>
              </Box>
            )}
          </VStack>
        </CardBody>
      </Card>
    </Container>
  );
};

export default TransactionTraceView;