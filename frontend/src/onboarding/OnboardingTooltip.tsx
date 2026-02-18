import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import type { TourStep } from './onboardingSteps';

interface TooltipProps {
    step: TourStep;
    stepNumber: number;
    totalSteps: number;
    isVisible: boolean;
    onNext: () => void;
    onSkip: () => void;
    onComplete: () => void;
    isLastStep: boolean;
}

interface Position {
    top: number;
    left: number;
}

const TOOLTIP_MARGIN = 16;
const VIEWPORT_PAD = 16;

function computePosition(
    targetSelector: string | null,
    placement: string,
    tooltipW: number,
    tooltipH: number,
): Position {
    const vw = window.innerWidth;
    const vh = window.innerHeight;

    if (!targetSelector || placement === 'center') {
        return {
            top: (vh - tooltipH) / 2,
            left: (vw - tooltipW) / 2,
        };
    }

    const el = document.querySelector(targetSelector);
    if (!el) {
        return { top: (vh - tooltipH) / 2, left: (vw - tooltipW) / 2 };
    }

    const rect = el.getBoundingClientRect();
    let top: number;
    let left: number;

    switch (placement) {
        case 'bottom':
            top = rect.bottom + TOOLTIP_MARGIN;
            left = rect.left + rect.width / 2 - tooltipW / 2;
            break;
        case 'top':
            top = rect.top - TOOLTIP_MARGIN - tooltipH;
            left = rect.left + rect.width / 2 - tooltipW / 2;
            break;
        case 'left':
            top = rect.top + rect.height / 2 - tooltipH / 2;
            left = rect.left - TOOLTIP_MARGIN - tooltipW;
            break;
        case 'right':
            top = rect.top + rect.height / 2 - tooltipH / 2;
            left = rect.right + TOOLTIP_MARGIN;
            break;
        default:
            top = rect.bottom + TOOLTIP_MARGIN;
            left = rect.left + rect.width / 2 - tooltipW / 2;
    }

    // Viewport clamping
    top = Math.max(VIEWPORT_PAD, Math.min(top, vh - tooltipH - VIEWPORT_PAD));
    left = Math.max(VIEWPORT_PAD, Math.min(left, vw - tooltipW - VIEWPORT_PAD));

    return { top, left };
}

export default function OnboardingTooltip({
    step,
    stepNumber,
    totalSteps,
    isVisible,
    onNext,
    onSkip,
    onComplete,
    isLastStep,
}: TooltipProps) {
    const [position, setPosition] = useState<Position>({ top: 0, left: 0 });
    const [measured, setMeasured] = useState(false);
    const tooltipRef = useRef<HTMLDivElement>(null);

    const updatePosition = useCallback(() => {
        const el = tooltipRef.current;
        const w = el ? el.offsetWidth : 380;
        const h = el ? el.offsetHeight : 200;
        setPosition(computePosition(step.targetSelector, step.placement, w, h));
        if (el) setMeasured(true);
    }, [step.targetSelector, step.placement]);

    useEffect(() => {
        if (!isVisible) {
            setMeasured(false);
            return;
        }

        // Initial position + poll for element
        let attempts = 0;
        const poll = setInterval(() => {
            updatePosition();
            if (++attempts > 20) clearInterval(poll);
        }, 100);

        window.addEventListener('scroll', updatePosition, true);
        window.addEventListener('resize', updatePosition);

        return () => {
            clearInterval(poll);
            window.removeEventListener('scroll', updatePosition, true);
            window.removeEventListener('resize', updatePosition);
        };
    }, [isVisible, updatePosition]);

    const isModal = step.placement === 'center';
    const isCompletion = step.id === 'completion';
    const isWelcome = step.id === 'welcome';

    return (
        <AnimatePresence mode="wait">
            {isVisible && (
                <>
                    {/* Backdrop for modal steps */}
                    {isModal && (
                        <motion.div
                            className="onboarding-modal-backdrop"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            style={{
                                position: 'fixed',
                                inset: 0,
                                background: 'rgba(0, 0, 0, 0.85)',
                                zIndex: 9998,
                            }}
                        />
                    )}

                    <motion.div
                        ref={tooltipRef}
                        key={step.id}
                        className={`onboarding-tooltip ${isModal ? 'onboarding-tooltip--modal' : ''}`}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: measured || isModal ? 1 : 0 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.25 }}
                        style={{
                            position: 'fixed',
                            top: position.top,
                            left: position.left,
                            zIndex: 10000,
                        }}
                    >
                        {/* Step indicator */}
                        {!isModal && (
                            <div className="onboarding-tooltip__step">
                                STEP {stepNumber + 1}/{totalSteps}
                            </div>
                        )}

                        {/* Title */}
                        <h3 className="onboarding-tooltip__title">
                            {isModal && <span className="onboarding-tooltip__prefix">&gt; </span>}
                            {step.title}
                        </h3>

                        {/* Description */}
                        <p className="onboarding-tooltip__desc">{step.description}</p>

                        {/* Completion footer */}
                        {isCompletion && (
                            <div className="onboarding-tooltip__footer">
                                Made with love by Samir Sarwar
                            </div>
                        )}

                        {/* Actions */}
                        <div className="onboarding-tooltip__actions">
                            {isWelcome ? (
                                <>
                                    <button className="onboarding-tooltip__btn onboarding-tooltip__btn--primary" onClick={onNext}>
                                        Begin Tour
                                    </button>
                                    <button className="onboarding-tooltip__btn onboarding-tooltip__btn--ghost" onClick={onSkip}>
                                        Skip Tour
                                    </button>
                                </>
                            ) : isCompletion ? (
                                <button className="onboarding-tooltip__btn onboarding-tooltip__btn--primary" onClick={onComplete}>
                                    Start Using CrisisRouter
                                </button>
                            ) : step.requireAction ? (
                                <button className="onboarding-tooltip__btn onboarding-tooltip__btn--ghost" onClick={onSkip}>
                                    Skip Tour
                                </button>
                            ) : (
                                <>
                                    <button className="onboarding-tooltip__btn onboarding-tooltip__btn--primary" onClick={onNext}>
                                        {isLastStep ? 'Finish' : 'Next'}
                                    </button>
                                    <button className="onboarding-tooltip__btn onboarding-tooltip__btn--ghost" onClick={onSkip}>
                                        Skip Tour
                                    </button>
                                </>
                            )}
                        </div>

                        {/* Progress dots */}
                        {!isModal && (
                            <div className="onboarding-tooltip__progress">
                                {Array.from({ length: totalSteps }, (_, i) => (
                                    <span
                                        key={i}
                                        className={`onboarding-tooltip__dot ${i === stepNumber ? 'onboarding-tooltip__dot--active' : ''} ${i < stepNumber ? 'onboarding-tooltip__dot--done' : ''}`}
                                    />
                                ))}
                            </div>
                        )}
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
