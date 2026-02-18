import { useState, useEffect } from 'react';
import { useOnboarding } from './useOnboarding';
import { ONBOARDING_STEPS } from './onboardingSteps';
import OnboardingSpotlight from './OnboardingSpotlight';
import OnboardingTooltip from './OnboardingTooltip';
import './onboarding.css';

export default function OnboardingOverlay() {
    const { isActive, currentStep, totalSteps, nextStep, skipTour, completeTour } = useOnboarding();
    const [showVisuals, setShowVisuals] = useState(false);

    const step = ONBOARDING_STEPS[currentStep];

    // Handle enterDelay
    useEffect(() => {
        if (!isActive || !step) {
            setShowVisuals(false);
            return;
        }

        if (step.enterDelay) {
            setShowVisuals(false);
            const timer = setTimeout(() => setShowVisuals(true), step.enterDelay);
            return () => clearTimeout(timer);
        } else {
            setShowVisuals(true);
        }
    }, [isActive, currentStep, step]);

    if (!isActive || !step) return null;

    return (
        <div className="onboarding-overlay">
            <OnboardingSpotlight
                targetSelector={step.targetSelector}
                isVisible={showVisuals}
                overlayOpacity={step.overlayOpacity}
            />
            <OnboardingTooltip
                step={step}
                stepNumber={currentStep}
                totalSteps={totalSteps}
                isVisible={showVisuals}
                onNext={nextStep}
                onSkip={skipTour}
                onComplete={completeTour}
                isLastStep={currentStep === totalSteps - 1}
            />
        </div>
    );
}
