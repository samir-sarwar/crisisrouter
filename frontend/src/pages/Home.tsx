import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import Map, { Layer, Source, Marker } from 'react-map-gl/mapbox';
import CrisisRequestForm from '../components/CrisisRequestForm';
import RequestPreviewCard from '../components/RequestPreviewCard';
import ActiveRequestCard from '../components/ActiveRequestCard';
import NotificationBell from '../components/NotificationBell';
import type { NearbyNotification } from '../components/NotificationBell';
import { generateFakeRequest } from '../utils/demoRequestGenerator';
import type { ActiveRequest } from '../utils/demoRequestGenerator';
import 'mapbox-gl/dist/mapbox-gl.css';

// WebSocket imports (uncomment for production):
// import SockJS from 'sockjs-client';
// import { Client } from '@stomp/stompjs';

interface FormData {
    title: string;
    address: string;
    description: string;
    severity: string;
    type: string;
    customCategory: string;
}

interface Category {
    id: string;
    name: string;
    description: string;
}

const severityMap: Record<string, number> = {
    low: 1,
    medium: 2,
    high: 3,
    critical: 4,
};

const getSessionViewport = () => {
    try {
        const raw = sessionStorage.getItem('crisisRouter.mapViewport');
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
};

const getDemoRequests = (): ActiveRequest[] => {
    try {
        const raw = sessionStorage.getItem('crisisRouter.demoRequests');
        return raw ? JSON.parse(raw) : [];
    } catch { return []; }
};

const saveDemoRequests = (requests: ActiveRequest[]) => {
    sessionStorage.setItem('crisisRouter.demoRequests', JSON.stringify(requests));
};

const INITIAL_FORM: FormData = {
    title: '',
    address: '',
    description: '',
    severity: 'medium',
    type: 'Medical',
    customCategory: '',
};

const Home: React.FC = () => {
    const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN;
    const mapRef = useRef<any>(null);
    const sessionViewport = getSessionViewport();
    const mapLoaded = useRef(false);
    const pendingUserFly = useRef<[number, number] | null>(null);

    // UI state
    const [isCreatingRequest, setIsCreatingRequest] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Categories from backend
    const [categories, setCategories] = useState<Category[]>([]);

    // Shared form state (lifted)
    const [formData, setFormData] = useState<FormData>(INITIAL_FORM);
    const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);

    // Geocoded location for preview card
    const [previewLocation, setPreviewLocation] = useState<{ lng: number; lat: number } | null>(null);

    // ── Active requests stored in local state ──────────────────
    const [activeRequests, setActiveRequests] = useState<ActiveRequest[]>([]);

    // Current user profile (for location, ownership checks, volunteer ID)
    const [currentUser, setCurrentUser] = useState<{
        id: string;
        firstName: string;
        lastName: string;
        latitude: number;
        longitude: number;
    } | null>(null);

    // Nearby request notifications
    const [notifications, setNotifications] = useState<NearbyNotification[]>([]);

    // Fetch Categories on Mount
    useEffect(() => {
        fetch('/api/categories')
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch categories');
                return res.json();
            })
            .then((data: Category[]) => setCategories(data))
            .catch(err => console.error('Categories fetch error:', err));
    }, []);

    // Fetch the current user's saved requests on mount (original working pattern)
    useEffect(() => {
        const fetchMyRequests = async () => {
            try {
                const res = await fetch('/api/requests/me', {
                    credentials: 'include',
                });
                if (!res.ok) return;
                const data = await res.json();

                let cats = categories;
                if (cats.length === 0) {
                    const catRes = await fetch('/api/categories');
                    if (catRes.ok) cats = await catRes.json();
                }

                const mapped = data.map((r: any) => {
                    const matchedCat = cats.find((c: Category) => c.id === r.categoryId);
                    return {
                        ...r,
                        type: r.customCategory || matchedCat?.name || 'General',
                        imageUrl: r.imageUrl || null,
                    };
                });

                // Include persisted demo requests from sessionStorage
                const demoRequests = getDemoRequests();
                setActiveRequests([...demoRequests, ...mapped]);
            } catch (err) {
                console.error('Failed to fetch saved requests:', err);
            }
        };
        fetchMyRequests();
    }, [categories]);

    // Fetch nearby requests (from other users) once we have coordinates
    useEffect(() => {
        if (!currentUser?.latitude || !currentUser?.longitude) return;

        const fetchNearby = async () => {
            try {
                const res = await fetch(
                    `/api/requests/nearby?latitude=${currentUser.latitude}&longitude=${currentUser.longitude}&radiusInMeters=10000`,
                    { credentials: 'include' }
                );
                if (!res.ok) return;
                const nearbyData = await res.json();

                let cats = categories;
                if (cats.length === 0) {
                    const catRes = await fetch('/api/categories');
                    if (catRes.ok) cats = await catRes.json();
                }

                setActiveRequests(prev => {
                    const existingIds = new Set(prev.map(r => r.id));
                    const newNearby = nearbyData
                        .filter((r: any) => !existingIds.has(r.id))
                        .map((r: any) => {
                            const matchedCat = cats.find((c: Category) => c.id === r.categoryId);
                            return {
                                ...r,
                                type: r.customCategory || matchedCat?.name || 'General',
                                imageUrl: r.imageUrl || null,
                            };
                        });
                    return [...prev, ...newNearby];
                });
            } catch (err) {
                console.error('Failed to fetch nearby requests:', err);
            }
        };
        fetchNearby();
    }, [currentUser, categories]);

    // Fetch user profile for spawn location + store for notifications
    useEffect(() => {
        fetch('/api/users/me', { credentials: 'include' })
            .then(res => {
                if (res.ok) return res.json();
                throw new Error('Not logged in');
            })
            .then(user => {
                setCurrentUser({
                    id: user.id,
                    firstName: user.firstName,
                    lastName: user.lastName,
                    latitude: user.latitude,
                    longitude: user.longitude,
                });
                // Only fly to saved location on true first visit (no sessionStorage viewport).
                // On subsequent visits or refreshes, sessionStorage restores position instantly.
                if (user.latitude && user.longitude && !sessionViewport) {
                    if (mapLoaded.current && mapRef.current) {
                        mapRef.current.flyTo({
                            center: [user.longitude, user.latitude],
                            zoom: 15.5,
                            duration: 2000,
                        });
                    } else {
                        // Map not ready yet — queue for onLoad
                        pendingUserFly.current = [user.longitude, user.latitude];
                    }
                }
            })
            .catch(() => { /* Ignore if not logged in or no location */ });
    }, []);

    // ── DEMO: Generate a fake nearby request every 3 minutes ──
    useEffect(() => {
        if (!currentUser?.latitude || !currentUser?.longitude) return;

        const interval = setInterval(async () => {
            try {
                const fakeRequest = await generateFakeRequest(
                    currentUser.latitude,
                    currentUser.longitude,
                    mapboxToken
                );

                // Add to active requests and persist demo requests to sessionStorage
                setActiveRequests(prev => {
                    const updated = [...prev, fakeRequest];
                    saveDemoRequests(updated.filter(r => r.id.toString().startsWith('demo-')));
                    return updated;
                });

                // Add notification
                setNotifications(prev => [{
                    id: fakeRequest.id,
                    title: fakeRequest.title,
                    address: fakeRequest.address,
                    severityLevel: fakeRequest.severityLevel,
                    latitude: fakeRequest.latitude,
                    longitude: fakeRequest.longitude,
                    timestamp: new Date(),
                    read: false,
                }, ...prev]);
            } catch (err) {
                console.error('Failed to generate demo request:', err);
            }
        }, 180_000);

        return () => clearInterval(interval);
    }, [currentUser, mapboxToken]);

    /*
     * ═══════════════════════════════════════════════════════════
     * REAL WEBSOCKET IMPLEMENTATION
     * Uncomment this block and comment out the demo interval
     * above to receive real-time requests from other users.
     * ═══════════════════════════════════════════════════════════
     */
    // useEffect(() => {
    //     if (!currentUser?.latitude || !currentUser?.longitude) return;
    //
    //     const client = new Client({
    //         webSocketFactory: () => new SockJS('/ws'),
    //         reconnectDelay: 5000,
    //         onConnect: () => {
    //             console.log('WebSocket connected');
    //             client.subscribe('/topic/requests', (message) => {
    //                 const newReq = JSON.parse(message.body);
    //
    //                 // Calculate distance from user to the new request (Haversine)
    //                 const R = 6371000; // Earth radius in meters
    //                 const dLat = (newReq.latitude - currentUser.latitude) * Math.PI / 180;
    //                 const dLon = (newReq.longitude - currentUser.longitude) * Math.PI / 180;
    //                 const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    //                     Math.cos(currentUser.latitude * Math.PI / 180) *
    //                     Math.cos(newReq.latitude * Math.PI / 180) *
    //                     Math.sin(dLon / 2) * Math.sin(dLon / 2);
    //                 const distance = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    //
    //                 // Only notify if within 3km and not the user's own request
    //                 if (distance <= 3000) {
    //                     const isOwn = newReq.creatorFirstName === currentUser.firstName
    //                         && newReq.creatorLastName === currentUser.lastName;
    //                     if (isOwn) return;
    //
    //                     const activeReq: ActiveRequest = {
    //                         ...newReq,
    //                         type: newReq.customCategory || 'General',
    //                         imageUrl: newReq.imageUrl || null,
    //                     };
    //
    //                     setActiveRequests(prev => {
    //                         if (prev.find(r => r.id === activeReq.id)) return prev;
    //                         return [...prev, activeReq];
    //                     });
    //
    //                     setNotifications(prev => [{
    //                         id: newReq.id,
    //                         title: newReq.title,
    //                         address: newReq.address,
    //                         severityLevel: newReq.severityLevel,
    //                         latitude: newReq.latitude,
    //                         longitude: newReq.longitude,
    //                         timestamp: new Date(),
    //                         read: false,
    //                     }, ...prev]);
    //                 }
    //             });
    //         },
    //         onDisconnect: () => console.log('WebSocket disconnected'),
    //         onStompError: (frame) => console.error('STOMP error:', frame),
    //     });
    //     client.activate();
    //     return () => { client.deactivate(); };
    // }, [currentUser]);

    // ── Notification handlers ──────────────────────────────────
    const handleNotificationClick = (notification: NearbyNotification) => {
        setNotifications(prev =>
            prev.map(n => n.id === notification.id ? { ...n, read: true } : n)
        );
        mapRef.current?.flyTo({
            center: [notification.longitude, notification.latitude],
            zoom: 16,
            pitch: 55,
            duration: 2000,
        });
    };

    const handleMarkAllRead = () => {
        setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    };

    const handleDismissNotification = (id: string) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    };

    // ── Volunteer handler ──────────────────────────────────────
    const handleVolunteer = async (requestId: string) => {
        // Demo fake requests have IDs starting with "demo-"
        if (requestId.startsWith('demo-')) {
            setActiveRequests(prev => {
                const updated = prev.map(r => r.id === requestId ? { ...r, status: 'CLAIMED' } : r);
                saveDemoRequests(updated.filter(r => r.id.toString().startsWith('demo-')));
                return updated;
            });

            // Build a demo claim so it appears in YourActions
            const req = activeRequests.find(r => r.id === requestId);
            if (req) {
                const demoClaim = {
                    id: `demo-claim-${crypto.randomUUID()}`,
                    resourceRequestId: req.id,
                    volunteerId: currentUser?.id ?? 'demo-user',
                    claimedAt: new Date().toISOString(),
                    status: 'ACTIVE',
                    requestTitle: req.title,
                    requestDescription: req.description,
                    requestAddress: req.address,
                    requestSeverityLevel: req.severityLevel,
                    requestImageUrl: req.imageUrl,
                    requestStatus: 'CLAIMED',
                    requestLatitude: req.latitude,
                    requestLongitude: req.longitude,
                    requesterFirstName: req.creatorFirstName,
                    requesterLastName: req.creatorLastName,
                    requesterEmail: `${req.creatorFirstName.toLowerCase()}@example.com`,
                    requesterPhone: null,
                };
                const existing = JSON.parse(sessionStorage.getItem('crisisRouter.demoClaims') || '[]');
                sessionStorage.setItem('crisisRouter.demoClaims', JSON.stringify([demoClaim, ...existing]));
            }
            return;
        }

        // Real request — call the Claim API
        if (!currentUser?.id) return;
        try {
            const res = await fetch(
                `/api/claims/request/${requestId}?volunteerId=${currentUser.id}`,
                { method: 'POST', credentials: 'include' }
            );
            if (res.ok) {
                setActiveRequests(prev =>
                    prev.map(r => r.id === requestId ? { ...r, status: 'CLAIMED' } : r)
                );
            }
        } catch (err) {
            console.error('Volunteer error:', err);
        }
    };

    // ── Map lifecycle handlers ─────────────────────────────────
    const handleMapLoad = useCallback(() => {
        mapLoaded.current = true;
        if (pendingUserFly.current) {
            mapRef.current?.flyTo({ center: pendingUserFly.current, zoom: 15.5, duration: 2000 });
            pendingUserFly.current = null;
        }
    }, []);

    const handleMoveEnd = useCallback(() => {
        if (!mapRef.current) return;
        const center = mapRef.current.getCenter();
        sessionStorage.setItem('crisisRouter.mapViewport', JSON.stringify({
            longitude: center.lng,
            latitude: center.lat,
            zoom: mapRef.current.getZoom(),
            pitch: mapRef.current.getPitch(),
            bearing: mapRef.current.getBearing(),
        }));
    }, []);

    // Debounced geocoding
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const geocodeAddress = useCallback(
        async (address: string) => {
            if (!address.trim() || !mapboxToken) return;
            try {
                const encoded = encodeURIComponent(address.trim());
                const res = await fetch(
                    `https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${mapboxToken}&limit=1`
                );
                const data = await res.json();
                if (data.features && data.features.length > 0) {
                    const [lng, lat] = data.features[0].center;
                    setPreviewLocation({ lng, lat });
                    mapRef.current?.flyTo({
                        center: [lng, lat],
                        zoom: 16,
                        pitch: 55,
                        duration: 2000,
                    });
                }
            } catch (err) {
                console.error('Geocoding error:', err);
            }
        },
        [mapboxToken]
    );

    // Watch address changes → debounced geocode
    useEffect(() => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (!formData.address.trim()) {
            setPreviewLocation(null);
            return;
        }
        debounceRef.current = setTimeout(() => {
            geocodeAddress(formData.address);
        }, 600);
        return () => {
            if (debounceRef.current) clearTimeout(debounceRef.current);
        };
    }, [formData.address, geocodeAddress]);

    // Thumbnail URL for preview card
    const thumbnailUrl = useMemo(() => {
        const imageFile = uploadedFiles.find(f => f.type.startsWith('image/'));
        return imageFile ? URL.createObjectURL(imageFile) : null;
    }, [uploadedFiles]);

    // Cancel handler — map stays where it is
    const handleCancel = () => {
        setIsCreatingRequest(false);
        setFormData(INITIAL_FORM);
        setUploadedFiles([]);
        setPreviewLocation(null);
    };

    // Submit handler — geocode final address, map category → UUID, POST to backend
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);

        try {
            // 1. Geocode the address for lat/lng
            let lat = previewLocation?.lat;
            let lng = previewLocation?.lng;

            if (!lat || !lng) {
                // Final geocode attempt if preview location wasn't set
                const encoded = encodeURIComponent(formData.address.trim());
                const geoRes = await fetch(
                    `https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${mapboxToken}&limit=1`
                );
                const geoData = await geoRes.json();
                if (geoData.features && geoData.features.length > 0) {
                    [lng, lat] = geoData.features[0].center;
                } else {
                    alert('Could not locate the address. Please enter a valid address.');
                    setIsSubmitting(false);
                    return;
                }
            }

            // 2. Map category name → UUID (re-fetch if empty)
            let cats = categories;
            if (cats.length === 0) {
                const catRes = await fetch('/api/categories');
                if (catRes.ok) cats = await catRes.json();
            }
            const matchedCategory = cats.find(
                c => c.name.toLowerCase() === formData.type.toLowerCase()
            );
            if (!matchedCategory) {
                console.error('Category match failed. type:', formData.type, 'categories:', cats);
                alert('Selected category not found. Please try again.');
                setIsSubmitting(false);
                return;
            }

            // 3. Fetch current user info for creatorFirstName/lastName
            const meRes = await fetch('/api/users/me', { credentials: 'include' });
            if (!meRes.ok) {
                alert('You must be logged in to submit a request. Redirecting to login...');
                window.location.href = '/';
                return;
            }
            const me = await meRes.json();

            // 4. Build the DTO
            const requestDTO = {
                title: formData.title,
                description: formData.description,
                address: formData.address,
                severityLevel: severityMap[formData.severity] || 2,
                categoryId: matchedCategory.id,
                customCategory: formData.type === 'Other' ? formData.customCategory : null,
                latitude: lat,
                longitude: lng,
                creatorFirstName: me.firstName || 'Unknown',
                creatorLastName: me.lastName || 'User',
                status: 'OPEN',
            };

            // 5. Build multipart form data
            const bodyFormData = new FormData();
            bodyFormData.append(
                'request',
                new Blob([JSON.stringify(requestDTO)], { type: 'application/json' })
            );

            const imageFile = uploadedFiles.find(f => f.type.startsWith('image/'));
            if (imageFile) {
                bodyFormData.append('image', imageFile);
            }

            // 6. POST to backend
            const response = await fetch('/api/requests', {
                method: 'POST',
                body: bodyFormData,
                credentials: 'include',
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || 'Failed to submit request');
            }

            const result = await response.json();
            console.log('Request created:', result);

            // Add the new request to local state so a marker appears immediately
            const newActiveRequest: ActiveRequest = {
                id: result.id,
                title: result.title,
                description: result.description,
                address: result.address,
                severityLevel: result.severityLevel,
                latitude: result.latitude,
                longitude: result.longitude,
                imageUrl: result.imageUrl || null,
                status: result.status,
                creatorFirstName: result.creatorFirstName,
                creatorLastName: result.creatorLastName,
                type: formData.type === 'Other' && formData.customCategory
                    ? formData.customCategory
                    : formData.type,
                customCategory: result.customCategory,
            };
            setActiveRequests(prev => {
                // Avoid duplicates (in case the mount fetch already loaded it)
                if (prev.find(r => r.id === newActiveRequest.id)) return prev;
                return [...prev, newActiveRequest];
            });

            // Success — close form and reset (map stays at location)
            handleCancel();
        } catch (error) {
            console.error('Submission error:', error);
            alert('Failed to submit request. See console for details.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!mapboxToken) {
        return (
            <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#000', color: '#fff' }}>
                Error: Mapbox token not found.
            </div>
        );
    }

    return (
        <div className="home-container">
            {/* Navbar */}
            <nav
                className="overlay-navbar"
                style={{
                    opacity: isCreatingRequest ? 0 : 1,
                    pointerEvents: isCreatingRequest ? 'none' : 'auto',
                    transition: 'opacity 0.3s',
                }}
            >
                <Link to="/your-requests" className="nav-link">Your Requests</Link>
                <Link to="/your-actions" className="nav-link">Your Actions</Link>
                <Link to="/your-profile" className="nav-link">Your Profile</Link>

                <NotificationBell
                    notifications={notifications}
                    onNotificationClick={handleNotificationClick}
                    onDismiss={handleDismissNotification}
                    onMarkAllRead={handleMarkAllRead}
                />
            </nav>

            {/* Full-screen map */}
            <Map
                ref={mapRef}
                initialViewState={
                    sessionViewport
                        ? {
                            longitude: sessionViewport.longitude,
                            latitude: sessionViewport.latitude,
                            zoom: sessionViewport.zoom,
                            pitch: sessionViewport.pitch,
                            bearing: sessionViewport.bearing,
                        }
                        : {
                            longitude: -79.3832,
                            latitude: 43.6532,
                            zoom: 15.5,
                            pitch: 60,
                            bearing: -17.6,
                        }
                }
                onLoad={handleMapLoad}
                onMoveEnd={handleMoveEnd}
                style={{ width: '100%', height: '100%' }}
                mapStyle="mapbox://styles/mapbox/dark-v11"
                mapboxAccessToken={mapboxToken}
            >
                <Source id="mapbox-dem" type="raster-dem" url="mapbox://mapbox.mapbox-terrain-dem-v1" tileSize={512} maxzoom={14} />
                <Layer
                    id="3d-buildings"
                    source="composite"
                    source-layer="building"
                    filter={['==', 'extrude', 'true']}
                    type="fill-extrusion"
                    minzoom={15}
                    paint={{
                        'fill-extrusion-color': '#aaa',
                        'fill-extrusion-height': ['interpolate', ['linear'], ['zoom'], 15, 0, 15.05, ['get', 'height']],
                        'fill-extrusion-base': ['interpolate', ['linear'], ['zoom'], 15, 0, 15.05, ['get', 'min_height']],
                        'fill-extrusion-opacity': 0.6,
                    }}
                />

                {/* Preview card marker on map (while creating) */}
                {isCreatingRequest && previewLocation && (
                    <Marker
                        longitude={previewLocation.lng}
                        latitude={previewLocation.lat}
                        anchor="bottom"
                    >
                        <RequestPreviewCard
                            title={formData.title}
                            address={formData.address}
                            type={formData.type === 'Other' && formData.customCategory ? formData.customCategory : formData.type}
                            severity={formData.severity}
                            description={formData.description}
                            thumbnailUrl={thumbnailUrl}
                        />
                    </Marker>
                )}

                {/* Render active requests as map markers */}
                {activeRequests.map(req => {
                    const isOwn = !req.id.toString().startsWith('demo-') && (
                        currentUser
                            ? req.creatorFirstName === currentUser.firstName
                            && req.creatorLastName === currentUser.lastName
                            : true
                    );

                    return (
                        <Marker
                            key={req.id}
                            longitude={req.longitude}
                            latitude={req.latitude}
                            anchor="bottom"
                        >
                            <ActiveRequestCard
                                title={req.title}
                                address={req.address}
                                type={req.type}
                                severity={String(req.severityLevel)}
                                description={req.description}
                                imageUrl={req.imageUrl}
                                creatorName={`${req.creatorFirstName} ${req.creatorLastName}`}
                                isOwnRequest={isOwn}
                                isVolunteering={req.status === 'CLAIMED'}
                                onVolunteer={() => handleVolunteer(req.id)}
                                onClick={() => {
                                    mapRef.current?.flyTo({
                                        center: [req.longitude, req.latitude],
                                        zoom: 16,
                                        pitch: 55,
                                        duration: 2000,
                                    });
                                }}
                            />
                        </Marker>
                    );
                })}
            </Map>

            {/* Right-side form panel */}
            <div className={`form-panel ${isCreatingRequest ? 'form-panel--open' : ''}`}>
                {isCreatingRequest && (
                    <CrisisRequestForm
                        onCancel={handleCancel}
                        formData={formData}
                        setFormData={setFormData}
                        uploadedFiles={uploadedFiles}
                        setUploadedFiles={setUploadedFiles}
                        onSubmit={handleSubmit}
                        isSubmitting={isSubmitting}
                        mapboxToken={mapboxToken}
                    />
                )}
            </div>

            {/* Primary Action Button */}
            <button
                className="create-request-btn"
                onClick={() => setIsCreatingRequest(true)}
                style={{
                    opacity: isCreatingRequest ? 0 : 1,
                    pointerEvents: isCreatingRequest ? 'none' : 'auto',
                    transform: isCreatingRequest ? 'translate(-50%, 20px)' : 'translate(-50%, 0)',
                }}
            >
                Create Crisis Request
            </button>

            <div className="scanline"></div>
        </div>
    );
};

export default Home;
