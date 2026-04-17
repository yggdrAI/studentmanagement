/**
 * Service Worker for Bennett SMS PWA
 * Enables offline support, caching, and fast loading
 */

const CACHE_VERSION = 'v1-bennett-sms';
const CACHE_URLS = [
  '/',
  '/student/dashboard',
  '/student/attendance',
  '/student/profile',
  '/css/dashboard.css',
  '/manifest.json'
];

// Install event - cache essential resources
self.addEventListener('install', (event) => {
  console.log('[ServiceWorker] Installing...');
  event.waitUntil(
    caches.open(CACHE_VERSION).then((cache) => {
      console.log('[ServiceWorker] Caching essential files');
      return cache.addAll(CACHE_URLS).catch(err => {
        console.warn('[ServiceWorker] Some files failed to cache:', err);
      });
    })
  );
  self.skipWaiting();
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('[ServiceWorker] Activating...');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_VERSION) {
            console.log('[ServiceWorker] Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// Fetch event - network first, fall back to cache
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip API calls - always fetch fresh
  if (url.pathname.includes('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          // Cache successful API responses
          if (response.ok && request.method === 'GET') {
            const responseClone = response.clone();
            caches.open(CACHE_VERSION).then((cache) => {
              cache.put(request, responseClone);
            });
          }
          return response;
        })
        .catch(() => {
          // Return cached response if network fails
          return caches.match(request).then((cachedResponse) => {
            return cachedResponse || new Response('Offline - Please check your connection', {
              status: 503,
              statusText: 'Service Unavailable'
            });
          });
        })
    );
    return;
  }

  // For other requests, try network first, then cache
  event.respondWith(
    fetch(request)
      .then((response) => {
        if (response.ok && request.method === 'GET') {
          const responseClone = response.clone();
          caches.open(CACHE_VERSION).then((cache) => {
            cache.put(request, responseClone);
          });
        }
        return response;
      })
      .catch(() => {
        return caches.match(request).then((cachedResponse) => {
          if (cachedResponse) {
            console.log('[ServiceWorker] Using cached:', request.url);
            return cachedResponse;
          }
          return new Response('Offline - Resource not available', {
            status: 503,
            statusText: 'Service Unavailable'
          });
        });
      })
  );
});

// Background sync for attendance marking
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-attendance') {
    console.log('[ServiceWorker] Syncing attendance...');
    event.waitUntil(
      // Sync logic here - will be called when online
      self.clients.matchAll().then((clients) => {
        clients.forEach((client) => {
          client.postMessage({ type: 'SYNC_ATTENDANCE' });
        });
      })
    );
  }
});

// Push notifications (for future alerts)
self.addEventListener('push', (event) => {
  if (!event.data) return;
  
  const data = event.data.json();
  const options = {
    body: data.body || 'Bennett SMS Notification',
    icon: '/icons/icon-192.png',
    badge: '/icons/badge-72.png',
    tag: 'bennett-sms',
    requireInteraction: false
  };

  event.waitUntil(
    self.registration.showNotification(data.title || 'Notification', options)
  );
});

console.log('[ServiceWorker] Loaded successfully');
