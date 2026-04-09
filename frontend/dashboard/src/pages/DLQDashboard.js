import React, { useEffect, useState, useCallback } from 'react';
import {
  Box,
  Container,
  VStack,
  HStack,
  Card,
  Button,
  Badge,
  Heading,
  Text,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Spinner,
  useToast,
  AlertDialog,
  AlertDialogBody,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogContent,
  AlertDialogOverlay,
  Input,
  FormControl,
  FormLabel,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
  ModalCloseButton,
  useDisclosure,
  Grid,
  GridItem,
  Progress,
  Stat,
  StatLabel,
  StatNumber,
  Divider,
} from '@chakra-ui/react';
import { ChevronRightIcon, RepeatIcon, AlertIcon } from '@chakra-ui/icons';
import apiClient from '../api/apiClient';

/**
 * DLQ Dashboard Component
 * 
 * Displays failed messages (Dead Letter Queue) for admin review and retry.
 * Features:
 * - Real-time DLQ statistics
 * - Failed messages table with pagination
 * - Retry button for each failed message
 * - Audit logging of all retry attempts
 * - Status indicators (PENDING, RETRYING, RESOLVED, DEAD)
 * 
 * Security:
 * - ADMIN role only (enforced by backend @PreAuthorize)
 * - All actions logged for audit trail
 * - User confirmation before retry operations
 */
const DLQDashboard = () => {
  // State management
  const [statistics, setStatistics] = useState(null);
  const [messages, setMessages] = useState([]);
  const [selectedStatus, setSelectedStatus] = useState('PENDING');
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [retrying, setRetrying] = useState(null);
  const [retryReason, setRetryReason] = useState('');
  
  const toast = useToast();
  const { isOpen: isRetryOpen, onOpen: onRetryOpen, onClose: onRetryClose } = useDisclosure();
  const { isOpen: isDeadOpen, onOpen: onDeadOpen, onClose: onDeadClose } = useDisclosure();
  const { isOpen: isReasonOpen, onOpen: onReasonOpen, onClose: onReasonClose } = useDisclosure();
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [deadReason, setDeadReason] = useState('');

  // Fetch DLQ statistics
  const fetchStatistics = useCallback(async () => {
    try {
      const response = await apiClient.get('/api/admin/dlq/statistics');
      setStatistics(response.data);
    } catch (error) {
      console.error('Error fetching DLQ statistics:', error);
      toast({
        title: 'Error',
        description: 'Failed to fetch DLQ statistics',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    }
  }, [toast]);

  // Fetch DLQ messages by status
  const fetchMessages = useCallback(async (status, page, size) => {
    setLoading(true);
    try {
      const response = await apiClient.get(`/api/admin/dlq/messages/status/${status}`, {
        params: {
          page,
          size,
        },
      });
      
      setMessages(response.data.content || []);
      setTotalElements(response.data.totalElements || 0);
      setCurrentPage(page);
    } catch (error) {
      console.error('Error fetching DLQ messages:', error);
      toast({
        title: 'Error',
        description: 'Failed to fetch DLQ messages',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  }, [toast]);

  // Retry a message
  const handleRetryMessage = useCallback(async (messageId) => {
    setRetrying(messageId);
    try {
      const response = await apiClient.post('/api/admin/dlq/messages/retry', {
        messageId,
        reason: retryReason,
      });

      toast({
        title: 'Success',
        description: `Message ${messageId} retry initiated successfully`,
        status: 'success',
        duration: 5000,
        isClosable: true,
      });

      // Refresh data
      setRetryReason('');
      onRetryClose();
      fetchStatistics();
      fetchMessages(selectedStatus, currentPage, pageSize);
    } catch (error) {
      console.error('Error retrying message:', error);
      const errorMsg = error.response?.status === 409 
        ? 'Message cannot be retried (not in PENDING status or max retries exceeded)'
        : 'Failed to initiate retry';
      
      toast({
        title: 'Error',
        description: errorMsg,
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    } finally {
      setRetrying(null);
    }
  }, [retryReason, selectedStatus, currentPage, pageSize, fetchStatistics, fetchMessages, toast, onRetryClose]);

  // Mark message as dead
  const handleMarkAsDead = useCallback(async (messageId) => {
    try {
      await apiClient.post(`/api/admin/dlq/messages/${messageId}/dead`, null, {
        params: {
          reason: deadReason,
        },
      });

      toast({
        title: 'Success',
        description: `Message ${messageId} marked as dead`,
        status: 'success',
        duration: 5000,
        isClosable: true,
      });

      setDeadReason('');
      onDeadOpen();
      fetchStatistics();
      fetchMessages(selectedStatus, currentPage, pageSize);
    } catch (error) {
      console.error('Error marking message as dead:', error);
      toast({
        title: 'Error',
        description: 'Failed to mark message as dead',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    }
  }, [deadReason, selectedStatus, currentPage, pageSize, fetchStatistics, fetchMessages, toast, onDeadOpen]);

  // Resolve a message
  const handleResolveMessage = useCallback(async (messageId) => {
    try {
      await apiClient.post(`/api/admin/dlq/messages/${messageId}/resolve`, null, {
        params: {
          notes: 'Manually resolved',
        },
      });

      toast({
        title: 'Success',
        description: `Message ${messageId} resolved`,
        status: 'success',
        duration: 5000,
        isClosable: true,
      });

      fetchStatistics();
      fetchMessages(selectedStatus, currentPage, pageSize);
    } catch (error) {
      console.error('Error resolving message:', error);
      toast({
        title: 'Error',
        description: 'Failed to resolve message',
        status: 'error',
        duration: 5000,
        isClosable: true,
      });
    }
  }, [selectedStatus, currentPage, pageSize, fetchStatistics, fetchMessages, toast]);

  // Initial data load
  useEffect(() => {
    fetchStatistics();
    fetchMessages(selectedStatus, 0, pageSize);
  }, [selectedStatus, pageSize, fetchStatistics, fetchMessages]);

  // Get status color
  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING':
        return 'yellow';
      case 'RETRYING':
        return 'blue';
      case 'RESOLVED':
        return 'green';
      case 'DEAD':
        return 'red';
      default:
        return 'gray';
    }
  };

  // Get message error truncated
  const truncateError = (error, length = 50) => {
    if (!error) return 'N/A';
    return error.length > length ? `${error.substring(0, length)}...` : error;
  };

  return (
    <Container maxW="7xl" py={8}>
      <VStack spacing={8} align="stretch">
        {/* Header */}
        <VStack align="flex-start" spacing={2}>
          <Heading as="h1" size="2xl" color="red.600">
            Dead Letter Queue Dashboard
          </Heading>
          <Text fontSize="md" color="gray.600">
            Monitor and manage failed messages requiring retry or investigation
          </Text>
        </VStack>

        {/* Statistics Section */}
        {statistics && (
          <Grid templateColumns="repeat(auto-fit, minmax(200px, 1fr))" gap={4}>
            <Card p={6}>
              <Stat>
                <StatLabel>Total Messages</StatLabel>
                <StatNumber color="gray.700">{statistics.totalMessages || 0}</StatNumber>
              </Stat>
            </Card>

            <Card p={6}>
              <Stat>
                <StatLabel>Pending (Needs Retry)</StatLabel>
                <StatNumber color="yellow.600">{statistics.pendingMessages || 0}</StatNumber>
              </Stat>
            </Card>

            <Card p={6}>
              <Stat>
                <StatLabel>Permanently Dead</StatLabel>
                <StatNumber color="red.600">{statistics.deadMessages || 0}</StatNumber>
              </Stat>
            </Card>

            <Card p={6}>
              <Stat>
                <StatLabel>Resolved</StatLabel>
                <StatNumber color="green.600">{statistics.resolvedMessages || 0}</StatNumber>
              </Stat>
            </Card>

            <Card p={6}>
              <Stat>
                <StatLabel>Success Rate</StatLabel>
                <StatNumber color="blue.600">
                  {(statistics.successRate || 0).toFixed(1)}%
                </StatNumber>
              </Stat>
            </Card>
          </Grid>
        )}

        <Divider />

        {/* Status Filter Buttons */}
        <HStack spacing={4}>
          {['PENDING', 'RETRYING', 'RESOLVED', 'DEAD'].map((status) => (
            <Button
              key={status}
              variant={selectedStatus === status ? 'solid' : 'outline'}
              colorScheme={selectedStatus === status ? getStatusColor(status) : 'gray'}
              onClick={() => {
                setSelectedStatus(status);
                setCurrentPage(0);
              }}
            >
              {status}
            </Button>
          ))}
        </HStack>

        {/* Messages Table */}
        <Card overflowX="auto">
          {loading ? (
            <HStack justify="center" py={10}>
              <Spinner size="lg" color="red.500" />
              <Text>Loading messages...</Text>
            </HStack>
          ) : messages.length === 0 ? (
            <HStack justify="center" py={10}>
              <Text color="gray.500">No messages found</Text>
            </HStack>
          ) : (
            <Box overflowX="auto">
              <Table variant="simple" size="sm">
                <Thead bg="gray.100">
                  <Tr>
                    <Th>Message ID</Th>
                    <Th>Transaction ID</Th>
                    <Th>Error</Th>
                    <Th>Retries</Th>
                    <Th>Status</Th>
                    <Th>Created</Th>
                    <Th>Actions</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {messages.map((msg) => (
                    <Tr key={msg.id} _hover={{ bg: 'gray.50' }}>
                      <Td fontWeight="bold">{msg.id}</Td>
                      <Td fontSize="xs" fontFamily="mono">
                        {msg.transactionId}
                      </Td>
                      <Td fontSize="xs" maxW="200px" title={msg.errorMessage}>
                        {truncateError(msg.errorMessage)}
                      </Td>
                      <Td>
                        <Progress
                          value={(msg.retryCount / msg.maxRetries) * 100}
                          size="sm"
                          colorScheme={msg.retryCount >= msg.maxRetries ? 'red' : 'yellow'}
                          title={`${msg.retryCount}/${msg.maxRetries}`}
                        />
                        <Text fontSize="xs" mt={1}>
                          {msg.retryCount}/{msg.maxRetries}
                        </Text>
                      </Td>
                      <Td>
                        <Badge colorScheme={getStatusColor(msg.status)}>
                          {msg.status}
                        </Badge>
                      </Td>
                      <Td fontSize="xs">
                        {msg.createdAt ? new Date(msg.createdAt).toLocaleString() : 'N/A'}
                      </Td>
                      <Td>
                        <HStack spacing={2}>
                          {msg.canRetry && (
                            <Button
                              size="xs"
                              colorScheme="blue"
                              leftIcon={<RepeatIcon />}
                              onClick={() => {
                                setSelectedMessage(msg);
                                onRetryOpen();
                              }}
                              isLoading={retrying === msg.id}
                            >
                              Retry
                            </Button>
                          )}
                          {msg.status === 'PENDING' && (
                            <Button
                              size="xs"
                              colorScheme="red"
                              variant="outline"
                              onClick={() => {
                                setSelectedMessage(msg);
                                onDeadOpen();
                              }}
                            >
                              Dead
                            </Button>
                          )}
                        </HStack>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </Box>
          )}
        </Card>

        {/* Pagination */}
        {!loading && totalElements > 0 && (
          <HStack justify="space-between" w="full">
            <Text fontSize="sm" color="gray.600">
              Showing {currentPage * pageSize + 1} to{' '}
              {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} messages
            </Text>
            <HStack>
              <Button
                size="sm"
                onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                isDisabled={currentPage === 0}
              >
                Previous
              </Button>
              <Text fontSize="sm">{currentPage + 1}</Text>
              <Button
                size="sm"
                onClick={() => setCurrentPage(prev => prev + 1)}
                isDisabled={(currentPage + 1) * pageSize >= totalElements}
              >
                Next
              </Button>
            </HStack>
          </HStack>
        )}
      </VStack>

      {/* Retry Modal with Reason */}
      <Modal isOpen={isRetryOpen} onClose={onRetryClose} isCentered>
        <ModalOverlay backdropFilter="blur(2px)" />
        <ModalContent>
          <ModalHeader>Confirm Retry</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            <VStack spacing={4}>
              <Text fontWeight="bold">
                Are you sure you want to retry message {selectedMessage?.id}?
              </Text>
              <Text fontSize="sm" color="gray.600">
                Transaction ID: {selectedMessage?.transactionId}
              </Text>
              <FormControl>
                <FormLabel>Reason for Retry (optional)</FormLabel>
                <Input
                  placeholder="Enter reason..."
                  value={retryReason}
                  onChange={(e) => setRetryReason(e.target.value)}
                />
              </FormControl>
            </VStack>
          </ModalBody>
          <ModalFooter>
            <Button variant="ghost" mr={3} onClick={onRetryClose}>
              Cancel
            </Button>
            <Button
              colorScheme="blue"
              onClick={() => handleRetryMessage(selectedMessage?.id)}
              isLoading={retrying === selectedMessage?.id}
            >
              Retry
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>

      {/* Mark as Dead Modal */}
      <Modal isOpen={isDeadOpen} onClose={onDeadClose} isCentered>
        <ModalOverlay backdropFilter="blur(2px)" />
        <ModalContent>
          <ModalHeader>Mark Message as Dead</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            <VStack spacing={4}>
              <HStack color="red.600">
                <AlertIcon />
                <Text fontWeight="bold">This action cannot be undone</Text>
              </HStack>
              <Text>
                Are you sure you want to mark message {selectedMessage?.id} as permanently dead?
              </Text>
              <FormControl>
                <FormLabel>Reason (required)</FormLabel>
                <Input
                  placeholder="Enter reason..."
                  value={deadReason}
                  onChange={(e) => setDeadReason(e.target.value)}
                />
              </FormControl>
            </VStack>
          </ModalBody>
          <ModalFooter>
            <Button variant="ghost" mr={3} onClick={onDeadClose}>
              Cancel
            </Button>
            <Button
              colorScheme="red"
              onClick={() => {
                handleMarkAsDead(selectedMessage?.id);
                onDeadClose();
              }}
            >
              Mark as Dead
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Container>
  );
};

export default DLQDashboard;
