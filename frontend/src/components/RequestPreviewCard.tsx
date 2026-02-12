import React from 'react';

interface RequestPreviewCardProps {
    title: string;
    address: string;
    type: string;
    severity: string;
    description: string;
    thumbnailUrl: string | null;
}

const severityColors: Record<string, string> = {
    low: '#3b82f6',
    medium: '#f59e0b',
    high: '#f97316',
    critical: '#ef4444',
};

const RequestPreviewCard: React.FC<RequestPreviewCardProps> = ({
    title,
    address,
    type,
    severity,
    description,
    thumbnailUrl,
}) => {
    const hasAnyContent = title || description || thumbnailUrl;

    return (
        <div className="preview-card">
            {/* Shine edge */}
            <div className="preview-card__shine" />

            {/* Thumbnail */}
            {thumbnailUrl && (
                <div className="preview-card__thumb">
                    <img src={thumbnailUrl} alt="Evidence" />
                </div>
            )}

            {/* Body */}
            <div className="preview-card__body">
                {/* Badges row — always visible since type/severity have defaults */}
                <div className="preview-card__badges">
                    <span className="preview-badge preview-badge--type">
                        {type.toUpperCase()}
                    </span>
                    <span
                        className="preview-badge preview-badge--severity"
                        style={{
                            background: `${severityColors[severity] || '#888'}22`,
                            color: severityColors[severity] || '#888',
                            borderColor: `${severityColors[severity] || '#888'}44`,
                        }}
                    >
                        {severity.toUpperCase()}
                    </span>
                </div>

                {/* Title */}
                {title ? (
                    <h3 className="preview-card__title">{title}</h3>
                ) : (
                    <h3 className="preview-card__title preview-card__title--empty">
                        Untitled Request
                    </h3>
                )}

                {/* Address */}
                {address && (
                    <div className="preview-card__address">
                        <span className="preview-card__address-icon">⌖</span>
                        {address}
                    </div>
                )}

                {/* Description */}
                {description && (
                    <p className="preview-card__desc">
                        {description.length > 120
                            ? description.slice(0, 120) + '…'
                            : description}
                    </p>
                )}

                {/* Empty state */}
                {!hasAnyContent && (
                    <p className="preview-card__empty">
                        Fill in the form to preview your request...
                    </p>
                )}
            </div>

            {/* Arrow pointer (bottom) */}
            <div className="preview-card__arrow" />
        </div>
    );
};

export default RequestPreviewCard;
