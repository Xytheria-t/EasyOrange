/**
 * @fileoverview WebSocket hook for AI consignment offer interactions.
 * Manages STOMP connection for sending offers and receiving results.
 */

import { useEffect, useRef, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import type { OfferResult } from '@/types';

const WS_URL = import.meta.env.VITE_WS_URL
    || `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`;
const HEARTBEAT_MS = 30000;
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000];

export type OfferDecisionCallback = (result: OfferResult) => void;

export interface UseOfferSocketReturn {
  sendOffer: (productId: string, offerPrice: number) => void;
  onOfferResult: (callback: OfferDecisionCallback) => void;
  isConnected: boolean;
}

export function useOfferSocket(): UseOfferSocketReturn {
  const clientRef = useRef<Client | null>(null);
  const reconnectAttemptRef = useRef(0);
  const callbackRef = useRef<OfferDecisionCallback | null>(null);
  const subscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const isConnectedRef = useRef(false);

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: {},
      heartbeatOutgoing: HEARTBEAT_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      reconnectDelay: RECONNECT_DELAYS[0],
      onConnect: () => {
        reconnectAttemptRef.current = 0;
        isConnectedRef.current = true;

        // Subscribe to the user's offer queue for results
        subscriptionRef.current = client.subscribe(
          '/user/queue/offers',
          (message: IMessage) => {
            try {
              const result: OfferResult = JSON.parse(message.body);
              if (callbackRef.current) {
                callbackRef.current(result);
              }
            } catch {
              // Failed to parse offer result
            }
          }
        );
      },
      onDisconnect: () => {
        isConnectedRef.current = false;
      },
      onWebSocketClose: () => {
        isConnectedRef.current = false;
      },
      beforeConnect: () => {
        const delay = RECONNECT_DELAYS[
          Math.min(reconnectAttemptRef.current, RECONNECT_DELAYS.length - 1)
        ];
        client.reconnectDelay = delay;
        reconnectAttemptRef.current++;
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
      client.deactivate();
      clientRef.current = null;
      isConnectedRef.current = false;
    };
  }, []);

  const sendOffer = useCallback(
    (productId: string, offerPrice: number) => {
      clientRef.current?.publish({
        destination: '/app/offer.make',
        body: JSON.stringify({ productId, offerPrice }),
      });
    },
    []
  );

  const onOfferResult = useCallback(
    (callback: OfferDecisionCallback) => {
      callbackRef.current = callback;
    },
    []
  );

  return {
    sendOffer,
    onOfferResult,
    get isConnected() { return isConnectedRef.current; },
  };
}
