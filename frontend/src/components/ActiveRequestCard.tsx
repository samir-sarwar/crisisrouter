import React, { useState } from 'react';

interface ActiveRequestCardProps {
    title: string;
    address: string;
    type: string;
    severity: string;
    description: string;
    imageUrl: string | null;
    creatorName: string;
    isOwnRequest: boolean;
    isVolunteering: boolean;
    onVolunteer?: () => void;
}

const severityColors: Record<string, string> = {
    '1': '#3b82f6',    // low — blue
    '2': '#f59e0b',    // medium — amber
    '3': '#f97316',    // high — orange
    '4': '#ef4444',    // critical — red
    low: '#3b82f6',
    medium: '#f59e0b',
    high: '#f97316',
    critical: '#ef4444',
};

const severityLabels: Record<string, string> = {
    '1': 'LOW',
    '2': 'MEDIUM',
    '3': 'HIGH',
    '4': 'CRITICAL',
};

const ActiveRequestCard: React.FC<ActiveRequestCardProps> = ({
    title,
    address,
    type,
    severity,
    description,
    imageUrl,
    creatorName,
    isOwnRequest,
    isVolunteering,
    onVolunteer,
}) => {
    const [isConfirming, setIsConfirming] = useState(false);
    const sevColor = severityColors[severity] || '#888';
    const sevLabel = severityLabels[severity] || severity?.toUpperCase() || 'UNKNOWN';

    return (
        <div className="active-card">
            {/* Pulsing red dot indicator */}
            <div className="active-card__pulse-wrapper">
                <span className="active-card__pulse-ring" />
                <span className="active-card__pulse-dot" />
            </div>

            {/* Shine on top edge */}
            <div className="active-card__shine" />

            {/* Optional thumbnail */}
            {imageUrl && (
                <div className="active-card__thumb">
                    <img src={imageUrl} alt="Evidence" />
                </div>
            )}

            {/* Body */}
            <div className="active-card__body">
                {/* Badges row */}
                <div className="active-card__badges">
                    <span className="active-badge active-badge--type">
                        {type?.toUpperCase() || 'GENERAL'}
                    </span>
                    <span
                        className="active-badge active-badge--severity"
                        style={{
                            background: `${sevColor}22`,
                            color: sevColor,
                            borderColor: `${sevColor}44`,
                        }}
                    >
                        {sevLabel}
                    </span>
                </div>

                {/* Title */}
                <h3 className="active-card__title">
                    {title || 'Untitled Request'}
                </h3>

                {/* Address */}
                {address && (
                    <div className="active-card__address">
                        <span className="active-card__address-icon">⌖</span>
                        {address}
                    </div>
                )}

                {/* Description */}
                {description && (
                    <p className="active-card__desc">
                        {description.length > 100
                            ? description.slice(0, 100) + '…'
                            : description}
                    </p>
                )}

                {/* Creator */}
                <div className="active-card__creator">
                    <span className="active-card__creator-icon">⊕</span>
                    {isOwnRequest ? 'You' : creatorName}
                </div>

                {/* Volunteer button — only on other users' requests */}
                {!isOwnRequest && (
                    isVolunteering ? (
                        <button className="active-card__volunteer-btn active-card__volunteer-btn--active">
                            ✔ Currently Volunteering
                        </button>
                    ) : (
                        !isConfirming ? (
                            <button
                                className="active-card__volunteer-btn"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setIsConfirming(true);
                                }}
                            >
                                ✦ Volunteer
                            </button>
                        ) : (
                            <div className="active-card__confirm-box" onClick={(e) => e.stopPropagation()}>
                                <span className="active-card__confirm-text">Are you sure?</span>
                                <div className="active-card__confirm-btns">
                                    <button
                                        className="active-card__confirm-btn active-card__confirm-btn--yes"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            onVolunteer?.();
                                            setIsConfirming(false);
                                        }}
                                    >
                                        YES
                                    </button>
                                    <button
                                        className="active-card__confirm-btn active-card__confirm-btn--no"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setIsConfirming(false);
                                        }}
                                    >
                                        NO
                                    </button>
                                </div>
                            </div>
                        )
                    )
                )}
            </div>

            {/* Arrow pointer */}
            <div className="active-card__arrow" />
        </div>
    );
};

export default ActiveRequestCard;
