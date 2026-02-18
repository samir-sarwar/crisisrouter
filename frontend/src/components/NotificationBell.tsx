import React, { useState, useRef, useEffect } from 'react';
import { Bell } from 'lucide-react';
import { useOnboarding } from '../onboarding/useOnboarding';

export interface NearbyNotification {
    id: string;
    title: string;
    address: string;
    severityLevel: number;
    latitude: number;
    longitude: number;
    timestamp: Date;
    read: boolean;
}

interface NotificationBellProps {
    notifications: NearbyNotification[];
    onNotificationClick: (notification: NearbyNotification) => void;
    onDismiss: (id: string) => void;
    onMarkAllRead: () => void;
}

const severityLabels: Record<number, string> = {
    1: 'LOW',
    2: 'MEDIUM',
    3: 'HIGH',
    4: 'CRITICAL',
};

function timeAgo(date: Date): string {
    const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
    if (seconds < 60) return 'just now';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
}

const NotificationBell: React.FC<NotificationBellProps> = ({
    notifications,
    onNotificationClick,
    onMarkAllRead,
}) => {
    const [isOpen, setIsOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);
    const { isActive, resolveAction } = useOnboarding();

    const unreadCount = notifications.filter(n => !n.read).length;

    // Close dropdown on click outside (disabled during onboarding to prevent accidental close)
    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (isActive) return;
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [isActive]);

    return (
        <div className="notif-bell" ref={ref}>
            <button
                className="notif-bell__btn"
                onClick={() => {
                    setIsOpen(prev => !prev);
                    if (isActive) resolveAction('open-notification-dropdown');
                }}
            >
                <Bell size={20} />
                {unreadCount > 0 && <span className="notif-bell__dot" />}
            </button>

            {isOpen && (
                <div className="notif-bell__dropdown">
                    <div className="notif-bell__header">
                        <span>Nearby Alerts ({unreadCount})</span>
                        {notifications.length > 0 && (
                            <button className="notif-bell__clear" onClick={onMarkAllRead}>
                                Clear
                            </button>
                        )}
                    </div>

                    <div className="notif-bell__list">
                        {notifications.length === 0 ? (
                            <div className="notif-bell__empty">
                                No nearby alerts yet
                            </div>
                        ) : (
                            notifications.map(n => (
                                <div
                                    key={n.id}
                                    className={`notif-bell__item ${!n.read ? 'notif-bell__item--unread' : ''}`}
                                    onClick={() => {
                                        onNotificationClick(n);
                                        setIsOpen(false);
                                        if (isActive) resolveAction('click-notification-item');
                                    }}
                                >
                                    <div className="notif-bell__item-title">{n.title}</div>
                                    <div className="notif-bell__item-address">{n.address}</div>
                                    <div className="notif-bell__item-meta">
                                        <span
                                            className="notif-bell__severity"
                                            data-level={n.severityLevel}
                                        >
                                            {severityLabels[n.severityLevel] || 'UNKNOWN'}
                                        </span>
                                        <span>{timeAgo(n.timestamp)}</span>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default NotificationBell;
