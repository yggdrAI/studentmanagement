import { useEffect, useMemo, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { createSeedSnapshot, mergeLiveEvent, normalizeSnapshot } from '../lib/analytics';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const SOCKET_URL = import.meta.env.VITE_SOCKET_URL || 'http://localhost:8080/ws';
const REFRESH_INTERVAL_MS = Number(import.meta.env.VITE_ANALYTICS_REFRESH_MS || 300000);

export function useLiveAnalytics() {
  const [snapshot, setSnapshot] = useState(() => createSeedSnapshot());
  const [connectionState, setConnectionState] = useState('booting');
  const clientRef = useRef(null);
  const refreshTimerRef = useRef(null);
  const liveStateRef = useRef(snapshot);

  useEffect(() => {
    liveStateRef.current = snapshot;
  }, [snapshot]);

  useEffect(() => {
    let active = true;

    const applySnapshot = (payload, source) => {
      if (!active) {
        return;
      }

      setSnapshot((current) => {
        const nextSnapshot = normalizeSnapshot({ ...payload, source }, current);
        liveStateRef.current = nextSnapshot;
        return nextSnapshot;
      });
    };

    const applyLiveEvent = (payload, source) => {
      if (!active) {
        return;
      }

      setSnapshot((current) => {
        const nextSnapshot = mergeLiveEvent(normalizeSnapshot({ ...payload, source }, current), payload);
        liveStateRef.current = nextSnapshot;
        return nextSnapshot;
      });
    };

    const loadBackendSnapshot = async () => {
      try {
        setConnectionState((current) => (current === 'live' ? current : 'syncing'));
        const response = await fetch(`${API_BASE_URL}/api/admin/dashboard/analytics`, {
          headers: { Accept: 'application/json' },
          credentials: 'include'
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const payload = await response.json();
        applySnapshot(payload, 'backend');
        return true;
      } catch (_error) {
        return false;
      }
    };

    const connectSocket = async () => {
      try {
        const client = new Client({
          webSocketFactory: () => new SockJS(SOCKET_URL, null, {
            transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
            withCredentials: true
          }),
          reconnectDelay: 4000,
          heartbeatIncoming: 8000,
          heartbeatOutgoing: 8000,
          debug: () => {}
        });

        client.onConnect = () => {
          setConnectionState('live');
          client.subscribe('/topic/analytics/live', (message) => {
            try {
              applySnapshot(JSON.parse(message.body), 'socket-live');
            } catch (_error) {
              // Ignore malformed frames and keep the last good snapshot.
            }
          });
          client.subscribe('/topic/analytics/feed', (message) => {
            try {
              applyLiveEvent(JSON.parse(message.body), 'socket-feed');
            } catch (_error) {
              // Ignore malformed frames and keep the last good snapshot.
            }
          });
        };

        client.onWebSocketClose = () => {
          setConnectionState((current) => (current === 'live' ? 'reconnecting' : 'offline'));
        };

        client.onStompError = () => {
          setConnectionState('offline');
        };

        client.activate();
        clientRef.current = client;
      } catch (_error) {
        setConnectionState('offline');
      }
    };

    void loadBackendSnapshot().then(() => {
      void connectSocket();
    });

    refreshTimerRef.current = window.setInterval(() => {
      void loadBackendSnapshot();
    }, REFRESH_INTERVAL_MS);

    return () => {
      active = false;
      if (refreshTimerRef.current) {
        window.clearInterval(refreshTimerRef.current);
      }
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, []);

  return useMemo(() => ({
    snapshot,
    connectionState,
    liveSummary: {
      lastUpdated: snapshot.updatedAt,
      source: snapshot.source
    }
  }), [snapshot, connectionState]);
}
