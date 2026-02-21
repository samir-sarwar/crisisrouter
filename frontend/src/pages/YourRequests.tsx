import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../utils/api';

/* ── Types ─────────────────────────────────────────────── */
interface RequestItem {
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
    categoryId: string;
    customCategory?: string;
    createdAt: string;
}

interface Volunteer {
    id: string;
    volunteerFirstName: string;
    volunteerLastName: string;
    volunteerEmail: string;
    volunteerPhone: string | null;
    claimedAt: string;
    status: string;
}

/* ── Severity helpers ──────────────────────────────────── */
const severityLabel = (n: number) =>
    ['', 'Low', 'Medium', 'High', 'Critical'][n] ?? `${n}`;
const severityClass = (n: number) =>
    ['', 'low', 'medium', 'high', 'critical'][n] ?? '';

/* ── Status badge helper ───────────────────────────────── */
const statusClass = (s: string) => {
    switch (s) {
        case 'OPEN': return 'yr-status--open';
        case 'CLAIMED': return 'yr-status--claimed';
        case 'FULFILLED': return 'yr-status--fulfilled';
        case 'CANCELLED': return 'yr-status--cancelled';
        default: return '';
    }
};

/* ── Format date ───────────────────────────────────────── */
const fmtDate = (iso: string) => {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
};

/* ═══════════════════════════════════════════════════════ */

export default function YourRequests() {
    const [requests, setRequests] = useState<RequestItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Per-request UI state
    const [editingId, setEditingId] = useState<string | null>(null);
    const [editForm, setEditForm] = useState({ title: '', description: '', address: '', severityLevel: 1 });
    const [expandedId, setExpandedId] = useState<string | null>(null);
    const [volunteers, setVolunteers] = useState<Record<string, Volunteer[]>>({});
    const [loadingVolunteers, setLoadingVolunteers] = useState<string | null>(null);

    /* ── Fetch requests on mount ── */
    const fetchRequests = useCallback(async () => {
        try {
            const res = await api('/api/requests/me');
            if (!res.ok) throw new Error('Failed to fetch requests');
            const data: RequestItem[] = await res.json();
            setRequests(data);
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchRequests(); }, [fetchRequests]);

    /* ── Partition ── */
    const active = requests.filter(r => r.status === 'OPEN' || r.status === 'CLAIMED');
    const past = requests.filter(r => r.status === 'FULFILLED' || r.status === 'CANCELLED');

    /* ── Edit handlers ── */
    const startEdit = (r: RequestItem) => {
        setEditingId(r.id);
        setEditForm({
            title: r.title,
            description: r.description,
            address: r.address,
            severityLevel: r.severityLevel,
        });
    };

    const cancelEdit = () => setEditingId(null);

    const saveEdit = async (id: string) => {
        try {
            const res = await api(`/api/requests/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(editForm),
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || 'Failed to update request');
            }
            const updated: RequestItem = await res.json();
            setRequests(prev => prev.map(r => r.id === id ? { ...r, ...updated } : r));
            setEditingId(null);
        } catch (err: any) {
            alert(err.message);
        }
    };

    /* ── Status actions ── */
    const changeStatus = async (id: string, status: string) => {
        const label = status === 'FULFILLED' ? 'complete' : 'cancel';
        if (!confirm(`Are you sure you want to ${label} this request?`)) return;
        try {
            const res = await api(`/api/requests/${id}?status=${status}`, {
                method: 'PATCH',
            });
            if (!res.ok) throw new Error('Failed to update status');
            const updated: RequestItem = await res.json();
            setRequests(prev => prev.map(r => r.id === id ? { ...r, ...updated } : r));
        } catch (err: any) {
            alert(err.message);
        }
    };

    /* ── Volunteers ── */
    const toggleVolunteers = async (requestId: string) => {
        if (expandedId === requestId) {
            setExpandedId(null);
            return;
        }
        setExpandedId(requestId);

        if (volunteers[requestId]) return; // already loaded

        setLoadingVolunteers(requestId);
        try {
            const res = await api(`/api/claims/request/${requestId}`);
            if (!res.ok) throw new Error('Failed to fetch volunteers');
            const data: Volunteer[] = await res.json();
            setVolunteers(prev => ({ ...prev, [requestId]: data }));
        } catch {
            setVolunteers(prev => ({ ...prev, [requestId]: [] }));
        } finally {
            setLoadingVolunteers(null);
        }
    };

    /* ═══════════════════════════════════════════════════════ */
    /* ── Render                                             ── */
    /* ═══════════════════════════════════════════════════════ */

    if (loading) {
        return (
            <div className="yr-page">
                <div className="yr-loading">Loading requests…</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="yr-page">
                <div className="yr-error">Error: {error}</div>
            </div>
        );
    }

    return (
        <div className="yr-page">
            {/* ── Header ── */}
            <header className="yr-header">
                <Link to="/home" className="yr-back">← Back to Map</Link>
                <h1 className="yr-title">&gt; YOUR_REQUESTS</h1>
                <p className="yr-subtitle">{requests.length} total request{requests.length !== 1 ? 's' : ''}</p>
            </header>

            {/* ── Active Requests ── */}
            <section className="yr-section">
                <h2 className="yr-section-heading">
                    <span className="yr-dot yr-dot--active" />
                    Active Requests
                    <span className="yr-count">{active.length}</span>
                </h2>

                {active.length === 0 && (
                    <p className="yr-empty">No active requests.</p>
                )}

                {active.map(req => (
                    <div key={req.id} className="yr-card">
                        {/* ── Top row: status + severity ── */}
                        <div className="yr-card__top">
                            <span className={`yr-status ${statusClass(req.status)}`}>{req.status}</span>
                            <span className={`yr-severity yr-severity--${severityClass(req.severityLevel)}`}>
                                {severityLabel(req.severityLevel)}
                            </span>
                        </div>

                        {editingId === req.id ? (
                            /* ── Edit mode ── */
                            <div className="yr-edit">
                                <label className="yr-edit__label">
                                    Title
                                    <input
                                        className="yr-edit__input"
                                        value={editForm.title}
                                        onChange={e => setEditForm(f => ({ ...f, title: e.target.value }))}
                                    />
                                </label>
                                <label className="yr-edit__label">
                                    Address
                                    <input
                                        className="yr-edit__input"
                                        value={editForm.address}
                                        onChange={e => setEditForm(f => ({ ...f, address: e.target.value }))}
                                    />
                                </label>
                                <label className="yr-edit__label">
                                    Severity (1–4)
                                    <input
                                        type="number"
                                        min={1}
                                        max={4}
                                        className="yr-edit__input yr-edit__input--sm"
                                        value={editForm.severityLevel}
                                        onChange={e => setEditForm(f => ({ ...f, severityLevel: Number(e.target.value) }))}
                                    />
                                </label>
                                <label className="yr-edit__label">
                                    Description
                                    <textarea
                                        className="yr-edit__textarea"
                                        value={editForm.description}
                                        onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))}
                                        rows={3}
                                    />
                                </label>
                                <div className="yr-edit__actions">
                                    <button className="yr-btn yr-btn--save" onClick={() => saveEdit(req.id)}>Save</button>
                                    <button className="yr-btn yr-btn--cancel" onClick={cancelEdit}>Cancel</button>
                                </div>
                            </div>
                        ) : (
                            /* ── Read mode ── */
                            <>
                                <h3 className="yr-card__title">{req.title}</h3>
                                <p className="yr-card__address">📍 {req.address}</p>
                                <p className="yr-card__desc">{req.description}</p>
                                {req.imageUrl && (
                                    <img src={req.imageUrl} alt="" className="yr-card__img" />
                                )}
                                <p className="yr-card__date">Created {fmtDate(req.createdAt)}</p>
                            </>
                        )}

                        {/* ── Action buttons ── */}
                        {editingId !== req.id && (
                            <div className="yr-card__actions">
                                {req.status === 'OPEN' && (
                                    <button className="yr-btn yr-btn--edit" onClick={() => startEdit(req)}>Edit</button>
                                )}
                                <button className="yr-btn yr-btn--complete" onClick={() => changeStatus(req.id, 'FULFILLED')}>
                                    ✓ Complete
                                </button>
                                <button className="yr-btn yr-btn--cancel-req" onClick={() => changeStatus(req.id, 'CANCELLED')}>
                                    ✕ Cancel
                                </button>
                                <button
                                    className={`yr-btn yr-btn--volunteers ${expandedId === req.id ? 'yr-btn--active' : ''}`}
                                    onClick={() => toggleVolunteers(req.id)}
                                >
                                    {expandedId === req.id ? '▾ Volunteers' : '▸ Volunteers'}
                                </button>
                            </div>
                        )}

                        {/* ── Volunteer list ── */}
                        {expandedId === req.id && (
                            <div className="yr-volunteers">
                                {loadingVolunteers === req.id && <p className="yr-volunteers__loading">Loading…</p>}
                                {volunteers[req.id]?.length === 0 && !loadingVolunteers && (
                                    <p className="yr-volunteers__empty">No volunteers yet.</p>
                                )}
                                {volunteers[req.id]?.map(v => (
                                    <div key={v.id} className="yr-volunteer-card">
                                        <div className="yr-volunteer-card__name">
                                            {v.volunteerFirstName} {v.volunteerLastName}
                                        </div>
                                        <div className="yr-volunteer-card__info">
                                            <span>✉ {v.volunteerEmail}</span>
                                            {v.volunteerPhone && <span>☎ {v.volunteerPhone}</span>}
                                        </div>
                                        <span className={`yr-status ${v.status === 'ACTIVE' ? 'yr-status--open' : 'yr-status--fulfilled'}`}>
                                            {v.status}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </section>

            {/* ── Past Requests ── */}
            <section className="yr-section">
                <h2 className="yr-section-heading">
                    <span className="yr-dot yr-dot--past" />
                    Past Requests
                    <span className="yr-count">{past.length}</span>
                </h2>

                {past.length === 0 && (
                    <p className="yr-empty">No past requests.</p>
                )}

                {past.map(req => (
                    <div key={req.id} className="yr-card yr-card--past">
                        <div className="yr-card__top">
                            <span className={`yr-status ${statusClass(req.status)}`}>{req.status}</span>
                            <span className={`yr-severity yr-severity--${severityClass(req.severityLevel)}`}>
                                {severityLabel(req.severityLevel)}
                            </span>
                        </div>
                        <h3 className="yr-card__title">{req.title}</h3>
                        <p className="yr-card__address">📍 {req.address}</p>
                        <p className="yr-card__desc">{req.description}</p>
                        {req.imageUrl && (
                            <img src={req.imageUrl} alt="" className="yr-card__img" />
                        )}
                        <p className="yr-card__date">Created {fmtDate(req.createdAt)}</p>

                        {/* Volunteers for past requests too */}
                        <div className="yr-card__actions">
                            <button
                                className={`yr-btn yr-btn--volunteers ${expandedId === req.id ? 'yr-btn--active' : ''}`}
                                onClick={() => toggleVolunteers(req.id)}
                            >
                                {expandedId === req.id ? '▾ Volunteers' : '▸ Volunteers'}
                            </button>
                        </div>

                        {expandedId === req.id && (
                            <div className="yr-volunteers">
                                {loadingVolunteers === req.id && <p className="yr-volunteers__loading">Loading…</p>}
                                {volunteers[req.id]?.length === 0 && !loadingVolunteers && (
                                    <p className="yr-volunteers__empty">No volunteers helped with this request.</p>
                                )}
                                {volunteers[req.id]?.map(v => (
                                    <div key={v.id} className="yr-volunteer-card">
                                        <div className="yr-volunteer-card__name">
                                            {v.volunteerFirstName} {v.volunteerLastName}
                                        </div>
                                        <div className="yr-volunteer-card__info">
                                            <span>✉ {v.volunteerEmail}</span>
                                            {v.volunteerPhone && <span>☎ {v.volunteerPhone}</span>}
                                        </div>
                                        <span className={`yr-status ${v.status === 'ACTIVE' ? 'yr-status--open' : 'yr-status--fulfilled'}`}>
                                            {v.status}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </section>
        </div>
    );
}
