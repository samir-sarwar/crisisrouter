import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../utils/api';

/* ── Types ─────────────────────────────────────────────── */
interface ClaimItem {
    id: string;
    resourceRequestId: string;
    volunteerId: string;
    claimedAt: string;
    status: string; // ACTIVE | FULFILLED | DROPPED

    // Request details
    requestTitle: string;
    requestDescription: string;
    requestAddress: string;
    requestSeverityLevel: number;
    requestImageUrl: string | null;
    requestStatus: string;
    requestLatitude: number | null;
    requestLongitude: number | null;

    // Requester contact
    requesterFirstName: string;
    requesterLastName: string;
    requesterEmail: string;
    requesterPhone: string | null;
}

/* ── Helpers ───────────────────────────────────────────── */
const severityLabel = (n: number) =>
    ['', 'Low', 'Medium', 'High', 'Critical'][n] ?? `${n}`;
const severityClass = (n: number) =>
    ['', 'low', 'medium', 'high', 'critical'][n] ?? '';

const claimStatusClass = (s: string) => {
    switch (s) {
        case 'ACTIVE': return 'ya-status--active';
        case 'FULFILLED': return 'ya-status--fulfilled';
        case 'DROPPED': return 'ya-status--dropped';
        default: return '';
    }
};

const fmtDate = (iso: string) => {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
};

const timeSince = (iso: string) => {
    if (!iso) return '';
    const diff = Date.now() - new Date(iso).getTime();
    const days = Math.floor(diff / 86400000);
    if (days > 0) return `${days}d ago`;
    const hours = Math.floor(diff / 3600000);
    if (hours > 0) return `${hours}h ago`;
    const mins = Math.floor(diff / 60000);
    return `${mins}m ago`;
};

/* ═══════════════════════════════════════════════════════ */

export default function YourActions() {
    const [claims, setClaims] = useState<ClaimItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [expandedContact, setExpandedContact] = useState<string | null>(null);

    /* ── Fetch claims on mount ── */
    const fetchClaims = useCallback(async () => {
        try {
            const res = await api('/api/claims/me');
            if (!res.ok) throw new Error('Failed to fetch claims');
            const data: ClaimItem[] = await res.json();

            // Merge demo claims from sessionStorage
            const demoClaims: ClaimItem[] = JSON.parse(
                sessionStorage.getItem('crisisRouter.demoClaims') || '[]'
            );
            setClaims([...demoClaims, ...data]);
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchClaims(); }, [fetchClaims]);

    /* ── Partition ── */
    const active = claims.filter(c => c.status === 'ACTIVE');
    const past = claims.filter(c => c.status === 'FULFILLED' || c.status === 'DROPPED');

    /* ── Drop claim ── */
    const handleDrop = async (claimId: string) => {
        if (!confirm('Are you sure you want to drop this volunteer commitment?')) return;

        // Handle demo claims via sessionStorage
        if (claimId.startsWith('demo-claim-')) {
            const demoClaims: ClaimItem[] = JSON.parse(
                sessionStorage.getItem('crisisRouter.demoClaims') || '[]'
            );
            const updated = demoClaims.map(c =>
                c.id === claimId ? { ...c, status: 'DROPPED', requestStatus: 'OPEN' } : c
            );
            sessionStorage.setItem('crisisRouter.demoClaims', JSON.stringify(updated));
            setClaims(prev => prev.map(c =>
                c.id === claimId ? { ...c, status: 'DROPPED', requestStatus: 'OPEN' } : c
            ));
            return;
        }

        try {
            const res = await api(`/api/claims/${claimId}/drop`, {
                method: 'PATCH',
            });
            if (!res.ok) throw new Error('Failed to drop claim');
            setClaims(prev => prev.map(c =>
                c.id === claimId ? { ...c, status: 'DROPPED', requestStatus: 'OPEN' } : c
            ));
        } catch (err: any) {
            alert(err.message);
        }
    };

    /* ── Toggle requester contact ── */
    const toggleContact = (claimId: string) => {
        setExpandedContact(prev => prev === claimId ? null : claimId);
    };

    /* ═══════════════════════════════════════════════════════ */
    /* ── Render                                             ── */
    /* ═══════════════════════════════════════════════════════ */

    if (loading) {
        return (
            <div className="ya-page">
                <div className="ya-loading">Loading your actions…</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="ya-page">
                <div className="ya-error">Error: {error}</div>
            </div>
        );
    }

    const renderCard = (claim: ClaimItem, isActive: boolean) => (
        <div key={claim.id} className={`ya-card ${!isActive ? 'ya-card--past' : ''}`}>
            {/* ── Top row ── */}
            <div className="ya-card__top">
                <span className={`ya-claim-status ${claimStatusClass(claim.status)}`}>{claim.status}</span>
                <span className={`ya-severity ya-severity--${severityClass(claim.requestSeverityLevel)}`}>
                    {severityLabel(claim.requestSeverityLevel)}
                </span>
                <span className="ya-card__time">Claimed {timeSince(claim.claimedAt)}</span>
            </div>

            {/* ── Request info ── */}
            <h3 className="ya-card__title">{claim.requestTitle}</h3>
            <p className="ya-card__address">📍 {claim.requestAddress}</p>
            <p className="ya-card__desc">{claim.requestDescription}</p>

            {claim.requestImageUrl && (
                <img src={claim.requestImageUrl} alt="" className="ya-card__img" />
            )}

            <p className="ya-card__date">Created {fmtDate(claim.claimedAt)}</p>

            {/* ── Actions ── */}
            <div className="ya-card__actions">
                {isActive && (
                    <button className="ya-btn ya-btn--drop" onClick={() => handleDrop(claim.id)}>
                        ✕ Drop Out
                    </button>
                )}

                <button
                    className={`ya-btn ya-btn--contact ${expandedContact === claim.id ? 'ya-btn--active' : ''}`}
                    onClick={() => toggleContact(claim.id)}
                >
                    {expandedContact === claim.id ? '▾ Requester Info' : '▸ Requester Info'}
                </button>

                {claim.requestLatitude && claim.requestLongitude && (
                    <Link
                        to={`/home?lat=${claim.requestLatitude}&lng=${claim.requestLongitude}`}
                        className="ya-btn ya-btn--map"
                    >
                        🗺 View on Map
                    </Link>
                )}
            </div>

            {/* ── Requester contact details ── */}
            {expandedContact === claim.id && (
                <div className="ya-contact">
                    <div className="ya-contact__card">
                        <div className="ya-contact__name">
                            {claim.requesterFirstName} {claim.requesterLastName}
                        </div>
                        <div className="ya-contact__info">
                            <span>✉ {claim.requesterEmail}</span>
                            {claim.requesterPhone && <span>☎ {claim.requesterPhone}</span>}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );

    return (
        <div className="ya-page">
            {/* ── Header ── */}
            <header className="ya-header">
                <Link to="/home" className="ya-back">← Back to Map</Link>
                <h1 className="ya-title">&gt; YOUR_ACTIONS</h1>
                <p className="ya-subtitle">{claims.length} total action{claims.length !== 1 ? 's' : ''}</p>
            </header>

            {/* ── Active Commitments ── */}
            <section className="ya-section">
                <h2 className="ya-section-heading">
                    <span className="ya-dot ya-dot--active" />
                    Active Commitments
                    <span className="ya-count">{active.length}</span>
                </h2>

                {active.length === 0 && (
                    <p className="ya-empty">No active commitments. Volunteer for a request from the map!</p>
                )}

                {active.map(c => renderCard(c, true))}
            </section>

            {/* ── Past Actions ── */}
            <section className="ya-section">
                <h2 className="ya-section-heading">
                    <span className="ya-dot ya-dot--past" />
                    Past Actions
                    <span className="ya-count">{past.length}</span>
                </h2>

                {past.length === 0 && (
                    <p className="ya-empty">No past actions yet.</p>
                )}

                {past.map(c => renderCard(c, false))}
            </section>
        </div>
    );
}
