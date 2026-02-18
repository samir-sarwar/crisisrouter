import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface SpotlightProps {
    targetSelector: string | null;
    isVisible: boolean;
    overlayOpacity?: number;
}

interface TargetRect {
    x: number;
    y: number;
    width: number;
    height: number;
}

const PADDING = 8;
const BORDER_RADIUS = 8;

export default function OnboardingSpotlight({ targetSelector, isVisible, overlayOpacity = 0.75 }: SpotlightProps) {
    const [rect, setRect] = useState<TargetRect | null>(null);

    const updateRect = useCallback(() => {
        if (!targetSelector) {
            setRect(null);
            return;
        }
        const el = document.querySelector(targetSelector);
        if (el) {
            const r = el.getBoundingClientRect();
            setRect({ x: r.x, y: r.y, width: r.width, height: r.height });
        }
    }, [targetSelector]);

    useEffect(() => {
        if (!isVisible || !targetSelector) {
            setRect(null);
            return;
        }

        let attempts = 0;
        const maxAttempts = 30;
        const pollInterval = setInterval(() => {
            const el = document.querySelector(targetSelector);
            if (el) {
                const r = el.getBoundingClientRect();
                setRect({ x: r.x, y: r.y, width: r.width, height: r.height });
                clearInterval(pollInterval);
            } else if (++attempts >= maxAttempts) {
                clearInterval(pollInterval);
            }
        }, 100);

        window.addEventListener('scroll', updateRect, true);
        window.addEventListener('resize', updateRect);

        return () => {
            clearInterval(pollInterval);
            window.removeEventListener('scroll', updateRect, true);
            window.removeEventListener('resize', updateRect);
        };
    }, [isVisible, targetSelector, updateRect]);

    // No spotlight for center/modal steps
    if (!targetSelector) return null;

    return (
        <AnimatePresence>
            {isVisible && rect && (
                <motion.div
                    className="onboarding-spotlight__glow"
                    initial={{ opacity: 0 }}
                    animate={{
                        opacity: 1,
                        left: rect.x - PADDING,
                        top: rect.y - PADDING,
                        width: rect.width + PADDING * 2,
                        height: rect.height + PADDING * 2,
                    }}
                    exit={{ opacity: 0 }}
                    transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                    style={{
                        position: 'fixed',
                        borderRadius: BORDER_RADIUS,
                        border: '2px solid #00ff41',
                        boxShadow:
                            `0 0 0 9999px rgba(0, 0, 0, ${overlayOpacity}), 0 0 15px rgba(0, 255, 65, 0.4), inset 0 0 15px rgba(0, 255, 65, 0.1)`,
                        zIndex: 9998,
                        pointerEvents: 'none',
                    }}
                />
            )}
        </AnimatePresence>
    );
}
