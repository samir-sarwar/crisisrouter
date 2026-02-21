import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useOnboarding } from '../onboarding/useOnboarding';
import { api } from '../utils/api';
import '../App.css';

interface UserProfile {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    description: string;
    address: string;
    profilePictureUrl: string | null;
    latitude: number | null;
    longitude: number | null;
}

export default function YourProfile() {
    const navigate = useNavigate();
    const { startTour } = useOnboarding();
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);
    const [imageFile, setImageFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);

    // Form state
    const [formData, setFormData] = useState({
        phoneNumber: '',
        description: '',
        address: '',
    });

    const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN;
    const [suggestions, setSuggestions] = useState<any[]>([]);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Fetch profile
    useEffect(() => {
        api('/api/users/me')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load profile');
                return res.json();
            })
            .then((data: UserProfile) => {
                setProfile(data);
                setFormData({
                    phoneNumber: data.phoneNumber || '',
                    description: data.description || '',
                    address: data.address || '',
                });
                if (data.profilePictureUrl) {
                    setPreviewUrl(data.profilePictureUrl);
                }
            })
            .catch(err => setMessage({ text: err.message, type: 'error' }))
            .finally(() => setLoading(false));
    }, []);

    // Handle Image Selection
    const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            setImageFile(file);
            setPreviewUrl(URL.createObjectURL(file));
        }
    };

    // Address Autocomplete
    const handleAddressChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const val = e.target.value;
        setFormData(prev => ({ ...prev, address: val }));

        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (!val.trim() || !mapboxToken) {
            setSuggestions([]);
            return;
        }

        debounceRef.current = setTimeout(async () => {
            try {
                const encoded = encodeURIComponent(val.trim());
                const res = await fetch(`https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${mapboxToken}&limit=5`);
                const data = await res.json();
                setSuggestions(data.features || []);
            } catch (err) {
                console.error("Geocoding error", err);
            }
        }, 300);
    };

    const selectSuggestion = (feature: any) => {
        setFormData(prev => ({ ...prev, address: feature.place_name }));
        setSuggestions([]);
    };

    // Submit
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);

        try {
            // Geocode if address changed to get new lat/long
            let lat = profile?.latitude;
            let lng = profile?.longitude;

            if (formData.address && formData.address !== (profile?.address || '') && mapboxToken) {
                const encoded = encodeURIComponent(formData.address.trim());
                const res = await fetch(`https://api.mapbox.com/geocoding/v5/mapbox.places/${encoded}.json?access_token=${mapboxToken}&limit=1`);
                const data = await res.json();
                if (data.features && data.features.length > 0) {
                    [lng, lat] = data.features[0].center;
                }
            }

            const profileDTO = {
                firstName: profile?.firstName, // Send back existing
                lastName: profile?.lastName,
                email: profile?.email,
                phoneNumber: formData.phoneNumber,
                description: formData.description,
                address: formData.address,
                latitude: lat,
                longitude: lng
            };

            const bodyFormData = new FormData();
            bodyFormData.append('user', new Blob([JSON.stringify(profileDTO)], { type: 'application/json' }));
            if (imageFile) {
                bodyFormData.append('image', imageFile);
            }

            const res = await api('/api/users/me', {
                method: 'PUT',
                body: bodyFormData,
            });

            if (!res.ok) throw new Error('Failed to update profile');

            const updated = await res.json();
            setProfile(updated);
            setMessage({ text: 'Profile updated successfully!', type: 'success' });
            // Clear stale viewport so Home.tsx flies to the new address on "Back to Map"
            sessionStorage.removeItem('crisisRouter.mapViewport');
            // Cleanup object URL
            if (imageFile && previewUrl && !updated.profilePictureUrl) {
                // If backend didn't return url but we have local preview
            }
        } catch (err: any) {
            setMessage({ text: err.message, type: 'error' });
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="yr-page"><div className="yr-loading">Loading Profile...</div></div>;

    return (
        <div className="yr-page">
            <header className="yr-header">
                <Link to="/home" className="yr-back">← Back to Map</Link>
                <h1 className="yr-title">&gt; YOUR_PROFILE</h1>
                <p className="yr-subtitle">Manage your identity and base of operations</p>
            </header>

            <div className="yr-section" style={{ maxWidth: '800px', margin: '0 auto' }}>
                <div className="yr-card">
                    {message && (
                        <div className={`yr-alert ${message.type === 'error' ? 'yr-alert--error' : 'yr-alert--success'}`}
                            style={{ padding: '1rem', marginBottom: '1rem', borderRadius: '4px', background: message.type === 'error' ? '#522' : '#252', border: `1px solid ${message.type === 'error' ? '#f55' : '#5f5'}` }}>
                            {message.text}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="yr-form">
                        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
                            {/* Profile Picture Column */}
                            <div style={{ flex: '0 0 200px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                                <div style={{
                                    width: '180px',
                                    height: '180px',
                                    borderRadius: '50%',
                                    overflow: 'hidden',
                                    background: '#111',
                                    border: '2px solid #333',
                                    marginBottom: '1rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center'
                                }}>
                                    {previewUrl ? (
                                        <img src={previewUrl} alt="Profile" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                    ) : (
                                        <span style={{ fontSize: '4rem', color: '#333' }}>?</span>
                                    )}
                                </div>
                                <label className="yr-btn" style={{ cursor: 'pointer', textAlign: 'center' }}>
                                    Change Photo
                                    <input type="file" accept="image/*" onChange={handleImageChange} style={{ display: 'none' }} />
                                </label>
                            </div>

                            {/* Fields Column */}
                            <div style={{ flex: 1, minWidth: '300px' }}>
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                                    <label className="yr-edit__label">
                                        First Name
                                        <input className="yr-edit__input" value={profile?.firstName || ''} disabled style={{ opacity: 0.7 }} />
                                    </label>
                                    <label className="yr-edit__label">
                                        Last Name
                                        <input className="yr-edit__input" value={profile?.lastName || ''} disabled style={{ opacity: 0.7 }} />
                                    </label>
                                </div>

                                <label className="yr-edit__label" style={{ marginBottom: '1rem' }}>
                                    Email
                                    <input className="yr-edit__input" value={profile?.email || ''} disabled style={{ opacity: 0.7 }} />
                                </label>

                                <label className="yr-edit__label" style={{ marginBottom: '1rem' }}>
                                    Phone Number
                                    <input
                                        className="yr-edit__input"
                                        value={formData.phoneNumber}
                                        onChange={e => setFormData({ ...formData, phoneNumber: e.target.value })}
                                        placeholder="+1 (555) 000-0000"
                                    />
                                </label>

                                <label className="yr-edit__label" style={{ marginBottom: '1rem', position: 'relative' }}>
                                    Default Address (Spawn Location)
                                    <input
                                        className="yr-edit__input"
                                        value={formData.address}
                                        onChange={handleAddressChange}
                                        placeholder="Start typing your address..."
                                        autoComplete="off"
                                    />
                                    {suggestions.length > 0 && (
                                        <ul style={{
                                            position: 'absolute',
                                            top: '100%',
                                            left: 0,
                                            right: 0,
                                            background: '#111',
                                            border: '1px solid #333',
                                            zIndex: 10,
                                            listStyle: 'none',
                                            margin: 0,
                                            padding: 0
                                        }}>
                                            {suggestions.map((s) => (
                                                <li key={s.id}
                                                    style={{ padding: '0.5rem 1rem', cursor: 'pointer', borderBottom: '1px solid #222' }}
                                                    onClick={() => selectSuggestion(s)}
                                                    onMouseEnter={(e) => e.currentTarget.style.background = '#222'}
                                                    onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                                                >
                                                    {s.place_name}
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </label>

                                <label className="yr-edit__label" style={{ marginBottom: '1rem' }}>
                                    About You
                                    <textarea
                                        className="yr-edit__textarea"
                                        value={formData.description}
                                        onChange={e => setFormData({ ...formData, description: e.target.value })}
                                        rows={4}
                                        placeholder="Tell us a bit about yourself..."
                                    />
                                </label>

                                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2rem' }}>
                                    <button type="submit" className="yr-btn yr-btn--save" disabled={saving}>
                                        {saving ? 'Saving...' : 'Save Profile'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            {/* Restart Tour */}
            <div style={{ maxWidth: '800px', margin: '2rem auto 0', textAlign: 'center' }}>
                <button
                    className="yr-btn"
                    style={{
                        background: 'transparent',
                        color: '#666',
                        border: '1px solid #333',
                        cursor: 'pointer',
                        fontSize: '0.75rem',
                        letterSpacing: '0.05em',
                    }}
                    onClick={() => {
                        localStorage.removeItem('crisisRouter.onboardingComplete');
                        navigate('/home');
                        setTimeout(() => startTour(), 500);
                    }}
                >
                    Restart Tour
                </button>
            </div>
        </div>
    );
}
