import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import Map, { Layer, Source, Marker } from 'react-map-gl/mapbox';
import CrisisRequestForm from '../components/CrisisRequestForm';
import RequestPreviewCard from '../components/RequestPreviewCard';
import ActiveRequestCard from '../components/ActiveRequestCard';
import 'mapbox-gl/dist/mapbox-gl.css';

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

// Represents a saved crisis request displayed on the map
interface ActiveRequest {
    id: string;
    title: string;
    description: string;
    address: string;
    severityLevel: number;
    latitude: number;
    longitude: number;
    imageUrl: string | null;
    status: string;
    creatorFirstName: string;
    creatorLastName: string;
    type: string;           // category display name
    customCategory?: string;
}

const severityMap: Record<string, number> = {
    low: 1,
    medium: 2,
    high: 3,
    critical: 4,
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

    // ── DEMO: Active requests stored in local state ──────────────────
    // In this demo version, submitted requests are added directly to
    // local state so the user sees their own requests immediately on
    // the map. Only requests from this session are shown.
    const [activeRequests, setActiveRequests] = useState<ActiveRequest[]>([]);

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

    // Fetch the current user's saved requests on mount
    useEffect(() => {
        const fetchMyRequests = async () => {
            try {
                const res = await fetch('/api/requests/me', {
                    credentials: 'include',
                });
                if (!res.ok) return; // silently skip if not authenticated yet
                const data = await res.json();

                // Wait for categories to resolve type names
                let cats = categories;
                if (cats.length === 0) {
                    const catRes = await fetch('/api/categories');
                    if (catRes.ok) cats = await catRes.json();
                }

                setActiveRequests(data.map((r: any) => {
                    const matchedCat = cats.find((c: Category) => c.id === r.categoryId);
                    return {
                        ...r,
                        type: r.customCategory || matchedCat?.name || 'General',
                        imageUrl: r.imageUrl || null,
                    };
                }));
            } catch (err) {
                console.error('Failed to fetch saved requests:', err);
            }
        };
        fetchMyRequests();
    }, [categories]);

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

            // ── DEMO: Add the new request to local state ─────────
            // The backend returns the full DTO with id, lat/lng, etc.
            // We push it into activeRequests so a marker appears on
            // the map immediately.
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
                <a href="#" className="nav-link">Your Profile</a>
            </nav>

            {/* Full-screen map */}
            <Map
                ref={mapRef}
                initialViewState={{
                    longitude: -79.3832,
                    latitude: 43.6532,
                    zoom: 15.5,
                    pitch: 60,
                    bearing: -17.6,
                }}
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

                {/* ── DEMO: Render saved requests as map markers ── */}
                {activeRequests.map(req => (
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
                            isOwnRequest={true} /* Demo: all requests are the user's own */
                        />
                    </Marker>
                ))}

                {/*
                 * ══════════════════════════════════════════════════════
                 * REAL IMPLEMENTATION (multi-user with API + WebSocket)
                 * ══════════════════════════════════════════════════════
                 *
                 * The code below is the production-ready version that:
                 *  1. Fetches ALL of the current user's requests from
                 *     the backend on mount via GET /api/requests/me
                 *  2. Subscribes to WebSocket topic /topic/requests so
                 *     new requests from ANY user appear in real-time
                 *  3. Compares creatorFirstName/LastName (or a userId
                 *     field if added later) to decide isOwnRequest
                 *  4. Shows a green "Volunteer" button on other users'
                 *     requests
                 *
                 * To enable: remove these comment blocks and delete or
                 * comment out the DEMO activeRequests state + submit
                 * handler additions above.
                 *
                 * ── Step 1: Fetch user's saved requests on mount ──
                 *
                 * useEffect(() => {
                 *     const fetchMyRequests = async () => {
                 *         try {
                 *             const res = await fetch('/api/requests/me', {
                 *                 credentials: 'include',
                 *             });
                 *             if (!res.ok) return;
                 *             const data = await res.json();
                 *             setActiveRequests(data.map((r: any) => ({
                 *                 ...r,
                 *                 type: r.customCategory || 'General',
                 *                 imageUrl: r.imageUrl || null,
                 *             })));
                 *         } catch (err) {
                 *             console.error('Failed to fetch requests:', err);
                 *         }
                 *     };
                 *     fetchMyRequests();
                 * }, []);
                 *
                 * ── Step 2: WebSocket subscription for live updates ──
                 *
                 * import SockJS from 'sockjs-client';
                 * import { Client } from '@stomp/stompjs';
                 *
                 * useEffect(() => {
                 *     const client = new Client({
                 *         webSocketFactory: () => new SockJS('/ws'),
                 *         onConnect: () => {
                 *             client.subscribe('/topic/requests', (message) => {
                 *                 const newReq = JSON.parse(message.body);
                 *                 setActiveRequests(prev => {
                 *                     // Avoid duplicates
                 *                     if (prev.find(r => r.id === newReq.id)) return prev;
                 *                     return [...prev, {
                 *                         ...newReq,
                 *                         type: newReq.customCategory || 'General',
                 *                         imageUrl: newReq.imageUrl || null,
                 *                     }];
                 *                 });
                 *             });
                 *         },
                 *     });
                 *     client.activate();
                 *     return () => { client.deactivate(); };
                 * }, []);
                 *
                 * ── Step 3: Determine ownership ──
                 * Fetch the current user's info once and compare:
                 *
                 * const [currentUser, setCurrentUser] = useState<{firstName: string; lastName: string} | null>(null);
                 *
                 * useEffect(() => {
                 *     fetch('/api/users/me', { credentials: 'include' })
                 *         .then(res => res.json())
                 *         .then(u => setCurrentUser({ firstName: u.firstName, lastName: u.lastName }))
                 *         .catch(() => {});
                 * }, []);
                 *
                 * // Then in the Marker render:
                 * const isOwn = currentUser
                 *     && req.creatorFirstName === currentUser.firstName
                 *     && req.creatorLastName === currentUser.lastName;
                 *
                 * <ActiveRequestCard
                 *     ...
                 *     isOwnRequest={isOwn}
                 *     onVolunteer={() => handleVolunteer(req.id)}
                 * />
                 *
                 * ── Step 4: Volunteer handler ──
                 *
                 * const handleVolunteer = async (requestId: string) => {
                 *     try {
                 *         const res = await fetch(`/api/requests/${requestId}/status?status=CLAIMED`, {
                 *             method: 'PATCH',
                 *             credentials: 'include',
                 *         });
                 *         if (res.ok) {
                 *             setActiveRequests(prev =>
                 *                 prev.map(r => r.id === requestId ? { ...r, status: 'CLAIMED' } : r)
                 *             );
                 *         }
                 *     } catch (err) {
                 *         console.error('Volunteer error:', err);
                 *     }
                 * };
                 *
                 * ══════════════════════════════════════════════════════
                 */}
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
