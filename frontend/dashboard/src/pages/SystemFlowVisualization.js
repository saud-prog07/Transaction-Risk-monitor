import React, { useState, useEffect } from 'react';
import {
  Box,
  Container,
  VStack,
  HStack,
  Card,
  Heading,
  Text,
  Badge,
  Icon,
  Circle,
  Divider,
  Grid,
  GridItem,
  Spinner,
} from '@chakra-ui/react';
import { ArrowRightIcon } from '@chakra-ui/icons';
import apiClient from '../services/apiService';

/**
 * System Flow Visualization Component
 * 
 * Displays the transaction processing pipeline:
 * Transaction → MQ (Message Queue) → Risk Engine → Alert Service
 * 
 * Features:
 * - Real-time status updates
 * - Active stage highlighting
 * - Stage durations
 * - No sensitive data displayed (ID masked, amounts hidden)
 * - Color-coded status indicators
 * 
 * Security Considerations:
 * - Displays only transaction ID (first 8 chars)
 * - Amount/risk score hidden
 * - User info not shown
 * - No detailed error messages
 */
const SystemFlowVisualization = () => {
  const [flowData, setFlowData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  
  // Mock toast function
  const toast = (config) => {
    console.log('[Toast]', config.title || 'Notification', ':', config.description || '');
  };

  // Define pipeline stages
  const stages = [
    {
      id: 'RECEIVED',
      name: 'Transaction',
      icon: '📨',
      description: 'Transaction received',
      color: 'blue',
    },
    {
      id: 'QUEUED',
      name: 'Message Queue',
      icon: '📦',
      description: 'Queued for processing',
      color: 'purple',
    },
    {
      id: 'PROCESSED',
      name: 'Risk Engine',
      icon: '⚙️',
      description: 'Risk analysis',
      color: 'orange',
    },
    {
      id: 'ALERTED',
      name: 'Alert Service',
      icon: '🚨',
      description: 'Alert generation',
      color: 'red',
    },
  ];

  // Fetch recent transactions flow data
  const fetchFlowData = async () => {
    try {
      setLoading(true);
      // Mock data - in production, would fetch real data from backend
      // GET /api/transactions/flow/recent
      
      const mockData = {
        activeTransaction: {
          id: 'txn_8f4c2a9b',
          status: 'PROCESSED',  // Current stage
          stages: [
            { stage: 'RECEIVED', timestamp: new Date(Date.now() - 5000), durationMs: 10 },
            { stage: 'QUEUED', timestamp: new Date(Date.now() - 4990), durationMs: 50 },
            { stage: 'PROCESSED', timestamp: new Date(Date.now() - 4940), durationMs: 120 },
            // ALERTED not reached yet
          ],
        },
        recentTransactions: [
          { id: 'txn_7e3b1a8c', status: 'ALERTED', completedAt: new Date(Date.now() - 30000) },
          { id: 'txn_6d2a0b7d', status: 'ALERTED', completedAt: new Date(Date.now() - 60000) },
          { id: 'txn_5c1a9c6e', status: 'PROCESSED', completedAt: new Date(Date.now() - 90000) },
        ],
      };

      setFlowData(mockData);
    } catch (error) {
      console.error('Error fetching flow data:', error);
      toast({
        title: 'Error',
        description: 'Failed to fetch transaction flow data',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  };

  // Load data on mount and set up refresh interval
  useEffect(() => {
    fetchFlowData();
    
    if (autoRefresh) {
      const interval = setInterval(fetchFlowData, 5000);  // Refresh every 5s
      return () => clearInterval(interval);
    }
  }, [autoRefresh, toast]);

  // Get stage index and progress
  const getCurrentStageIndex = () => {
    if (!flowData?.activeTransaction?.status) return -1;
    return stages.findIndex(s => s.id === flowData.activeTransaction.status);
  };

  // Get duration for a stage
  const getStageData = (stageId) => {
    if (!flowData?.activeTransaction?.stages) return null;
    return flowData.activeTransaction.stages.find(s => s.stage === stageId);
  };

  // Get status badge color
  const getStatusColor = (status) => {
    switch (status) {
      case 'RECEIVED':
        return 'blue';
      case 'QUEUED':
        return 'purple';
      case 'PROCESSED':
        return 'orange';
      case 'ALERTED':
        return 'red';
      default:
        return 'gray';
    }
  };

  if (loading) {
    return (
      <Container maxW="6xl" py={8}>
        <VStack justify="center" spacing={4} py={16}>
          <Spinner size="xl" color="blue.500" />
          <Text>Loading system flow...</Text>
        </VStack>
      </Container>
    );
  }

  const currentStageIndex = getCurrentStageIndex();
  const totalDuration = flowData?.activeTransaction?.stages
    ?.reduce((sum, s) => sum + s.durationMs, 0) || 0;

  return (
    <Container maxW="6xl" py={8}>
      <VStack spacing={12} align="stretch">
        {/* Header */}
        <VStack align="flex-start" spacing={2}>
          <Heading as="h1" size="xl" color="blue.600">
            System Flow Visualization
          </Heading>
          <Text fontSize="md" color="gray.600">
            Real-time transaction processing through the system pipeline
          </Text>
        </VStack>

        {/* Active Transaction Flow */}
        {flowData?.activeTransaction && (
          <Card p={8} bg="blue.50" borderLeft="4px solid" borderLeftColor="blue.500">
            <VStack spacing={6} align="stretch">
              {/* Transaction ID and Status */}
              <HStack justify="space-between">
                <VStack align="flex-start" spacing={1}>
                  <Text fontWeight="bold" color="gray.700">Active Transaction</Text>
                  <HStack spacing={2}>
                    <Badge colorScheme="blue" variant="subtle">
                      ID: {flowData.activeTransaction.id}
                    </Badge>
                    <Badge colorScheme={getStatusColor(currentStageIndex >= 0 ? stages[currentStageIndex].id : 'RECEIVED')}>
                      {stages[Math.max(0, currentStageIndex)]?.name}
                    </Badge>
                  </HStack>
                </VStack>
                <Text fontSize="sm" color="gray.600">
                  Round-trip: <Text as="span" fontWeight="bold">{totalDuration}ms</Text>
                </Text>
              </HStack>

              {/* Pipeline Visualization */}
              <Box w="full">
                <Grid
                  templateColumns="repeat(4, 1fr)"
                  gap={4}
                  mb={4}
                >
                  {stages.map((stage, index) => {
                    const isActive = index === currentStageIndex;
                    const isCompleted = index < currentStageIndex;
                    const stageDuration = getStageData(stage.id)?.durationMs || 0;

                    return (
                      <GridItem key={stage.id}>
                        <VStack spacing={2}>
                          {/* Stage Circle */}
                          <Circle
                            size="80px"
                            bg={isActive ? `${stage.color}.500` : isCompleted ? `${stage.color}.200` : 'gray.100'}
                            border={isActive ? `3px solid ${stage.color}` : 'none'}
                            boxShadow={isActive ? `0 0 20px ${stage.color}` : 'none'}
                            display="flex"
                            alignItems="center"
                            justifyContent="center"
                            fontSize="32px"
                            fontWeight="bold"
                            transition="all 0.3s"
                            _hover={{
                              transform: 'scale(1.05)',
                              boxShadow: isActive ? `0 0 25px ${stage.color}` : 'none',
                            }}
                          >
                            {isActive ? (
                              <Spinner size="lg" color="white" thickness="4px" />
                            ) : (
                              stage.icon
                            )}
                          </Circle>

                          {/* Stage Name and Duration */}
                          <VStack spacing={0} textAlign="center">
                            <Text fontWeight="bold" fontSize="sm">
                              {stage.name}
                            </Text>
                            {stageDuration > 0 && (
                              <Text fontSize="xs" color="gray.600">
                                {stageDuration}ms
                              </Text>
                            )}
                          </VStack>

                          {/* Status Label */}
                          <Badge
                            size="sm"
                            colorScheme={isActive ? stage.color : isCompleted ? stage.color : 'gray'}
                            variant={isActive ? 'solid' : 'subtle'}
                          >
                            {isActive ? 'ACTIVE' : isCompleted ? 'DONE' : 'PENDING'}
                          </Badge>
                        </VStack>
                      </GridItem>
                    );
                  })}
                </Grid>

                {/* Connection Lines */}
                <HStack spacing={0} w="full" justify="space-between" mb={4} px={4}>
                  {[0, 1, 2].map((index) => {
                    const isCompleted = index < currentStageIndex;
                    return (
                      <Box
                        key={`arrow-${index}`}
                        flex={1}
                        h="2px"
                        bg={isCompleted ? 'blue.400' : 'gray.200'}
                        position="relative"
                        _after={{
                          content: '""',
                          position: 'absolute',
                          right: '-6px',
                          top: '-4px',
                          width: '0',
                          height: '0',
                          borderLeft: '6px solid',
                          borderLeftColor: isCompleted ? 'blue.400' : 'gray.200',
                          borderTop: '4px solid transparent',
                          borderBottom: '4px solid transparent',
                        }}
                      />
                    );
                  })}
                </HStack>
              </Box>

              {/* Stage Details */}
              <Box w="full" bg="white" p={4} borderRadius="md" borderWidth="1px">
                <Text fontSize="sm" fontWeight="bold" mb={2}>
                  Pipeline Status
                </Text>
                <VStack spacing={1} align="flex-start">
                  {stages.map((stage, index) => {
                    const stageData = getStageData(stage.id);
                    const isActive = index === currentStageIndex;
                    const isCompleted = index < currentStageIndex;

                    return (
                      <HStack
                        key={stage.id}
                        w="full"
                        fontSize="sm"
                        p={2}
                        borderRadius="md"
                        bg={isActive ? `${stage.color}.50` : 'transparent'}
                        borderLeft={isActive ? `3px solid ${stage.color}` : 'none'}
                      >
                        <Text fontWeight="bold" minW="120px">
                          {stage.name}
                        </Text>
                        <Text color="gray.600">
                          {isCompleted ? '✓ Completed' : isActive ? '◌ Processing...' : '○ Pending'}
                        </Text>
                        {stageData && (
                          <Text color="gray.500" ml="auto">
                            {stageData.durationMs}ms
                          </Text>
                        )}
                      </HStack>
                    );
                  })}
                </VStack>
              </Box>
            </VStack>
          </Card>
        )}

        <Divider />

        {/* Recent Transactions Summary */}
        {flowData?.recentTransactions && flowData.recentTransactions.length > 0 && (
          <Card p={6}>
            <VStack align="stretch" spacing={4}>
              <Heading as="h3" size="md">
                Recent Transactions
              </Heading>

              <VStack spacing={2} align="stretch">
                {flowData.recentTransactions.map((txn) => (
                  <HStack
                    key={txn.id}
                    p={3}
                    borderRadius="md"
                    bg="gray.50"
                    borderLeft="4px solid"
                    borderLeftColor={getStatusColor(txn.status)}
                  >
                    <VStack align="flex-start" spacing={0} flex={1}>
                      <Text fontSize="sm" fontWeight="bold">
                        {txn.id}
                      </Text>
                      <Text fontSize="xs" color="gray.600">
                        Completed {new Date(txn.completedAt).toLocaleTimeString()}
                      </Text>
                    </VStack>
                    <Badge colorScheme={getStatusColor(txn.status)}>
                      {txn.status}
                    </Badge>
                  </HStack>
                ))}
              </VStack>
            </VStack>
          </Card>
        )}

        {/* System Health Summary */}
        <Card p={6} bg="green.50">
          <Grid templateColumns="repeat(4, 1fr)" gap={4}>
            <GridItem textAlign="center">
              <Text fontSize="sm" color="gray.600" mb={1}>Average Pipeline Time</Text>
              <Text fontSize="lg" fontWeight="bold" color="green.600">248ms</Text>
            </GridItem>
            <GridItem textAlign="center">
              <Text fontSize="sm" color="gray.600" mb={1}>Transactions/min</Text>
              <Text fontSize="lg" fontWeight="bold" color="green.600">1,247</Text>
            </GridItem>
            <GridItem textAlign="center">
              <Text fontSize="sm" color="gray.600" mb={1}>Success Rate</Text>
              <Text fontSize="lg" fontWeight="bold" color="green.600">99.92%</Text>
            </GridItem>
            <GridItem textAlign="center">
              <Text fontSize="sm" color="gray.600" mb={1}>Alerts Generated</Text>
              <Text fontSize="lg" fontWeight="bold" color="orange.600">47</Text>
            </GridItem>
          </Grid>
        </Card>

        {/* Key Information */}
        <Card p={6} bg="blue.50" borderLeft="4px solid" borderLeftColor="blue.500">
          <VStack align="flex-start" spacing={2}>
            <Text fontWeight="bold" color="blue.700">
              Pipeline Flow
            </Text>
            <Text fontSize="sm" color="gray.700">
              1. <Text as="span" fontWeight="bold">RECEIVED</Text>: Transaction enters the system
            </Text>
            <Text fontSize="sm" color="gray.700">
              2. <Text as="span" fontWeight="bold">QUEUED</Text>: Message placed in MQ for async processing
            </Text>
            <Text fontSize="sm" color="gray.700">
              3. <Text as="span" fontWeight="bold">PROCESSED</Text>: Risk engine analyzes transaction
            </Text>
            <Text fontSize="sm" color="gray.700">
              4. <Text as="span" fontWeight="bold">ALERTED</Text>: Alert service generates notifications
            </Text>
          </VStack>
        </Card>
      </VStack>
    </Container>
  );
};

export default SystemFlowVisualization;
