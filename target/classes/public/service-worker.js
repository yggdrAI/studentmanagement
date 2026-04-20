/**
 * Service Worker for Bennett SMS PWA
 * Enables offline support, caching, and fast loading
 */

const CACHE_VERSION = 'v5-bennett-sms';
const CACHE_URLS = [
  '/',
  '/login',
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

  // Only handle same-origin GET requests.
  if (request.method !== 'GET' || url.origin !== self.location.origin) {
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith(handleNavigationRequest(request));
    return;
  }

  // Always serve admin dashboard assets fresh to avoid stale UI after deployments.
  if (
    request.method === 'GET' && (
      url.pathname === '/admin/dashboard' ||
      url.pathname === '/js/admin-dashboard.js' ||
      url.pathname === '/css/admin-dashboard.css'
    )
  ) {
    event.respondWith(fetch(request));
    return;
  }

  // API calls: network-first, fall back to cache if available.
  if (url.pathname.includes('/api/')) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok && response.type === 'basic') {
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

  // Static assets/pages: network-first, then cache.
  event.respondWith(
    fetch(request)
      .then((response) => {
        if (response.ok && response.type === 'basic') {
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
            return cachedResponse;
          }

          return new Response(JSON.stringify({
            status: 'offline',
            message: 'Resource unavailable while offline'
          }), {
            status: 503,
            headers: { 'Content-Type': 'application/json; charset=utf-8' }
          });
        });
      })
  );
});

async function handleNavigationRequest(request) {
  try {
    const networkResponse = await fetch(request);
    if (networkResponse && networkResponse.ok && networkResponse.type === 'basic') {
      // Never fail navigation if cache write fails for dynamic HTML responses.
      try {
        const cache = await caches.open(CACHE_VERSION);
        await cache.put(request, networkResponse.clone());
      } catch (cacheError) {
        console.warn('[ServiceWorker] Navigation cache write failed:', cacheError);
      }
    }
    return networkResponse;
  } catch (_error) {
    const cached = await caches.match(request);
    if (cached) {
      return cached;
    }

    const appShell = await caches.match('/') || await caches.match('/login');
    if (appShell) {
      return appShell;
    }

    return new Response(`<!doctype html>
      <html lang="en">
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Offline</title>
        <style>
          body { margin: 0; font-family: system-ui, sans-serif; background: #0f172a; color: #e2e8f0; min-height: 100vh; display: grid; place-items: center; }
          .card { max-width: 28rem; padding: 24px; border-radius: 16px; background: rgba(15, 23, 42, 0.88); border: 1px solid rgba(148, 163, 184, 0.2); box-shadow: 0 18px 40px rgba(0, 0, 0, 0.25); }
          h1 { margin: 0 0 10px; font-size: 1.4rem; }
          p { margin: 0; line-height: 1.5; color: #cbd5e1; }
        </style>
      </head>
      <body>
        <div class="card">
          <h1>Offline mode</h1>
          <p>The requested page is not cached yet. Reconnect once to load the latest app shell.</p>
        </div>
      </body>
      </html>`, {
      status: 503,
      headers: { 'Content-Type': 'text/html; charset=utf-8' }
    });
  }
}

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
