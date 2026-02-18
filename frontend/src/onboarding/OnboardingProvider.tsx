import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { OnboardingContext } from './useOnboarding';
import type { HomeCallbacks } from './useOnboarding';
import { ONBOARDING_STEPS } from './onboardingSteps';
import type { TourStepContext } from './onboardingSteps';

const STORAGE_KEY = 'crisisRouter.onboardingComplete';

export default function OnboardingProvider({ children }: { children: React.ReactNode }) {
    const [isActive, setIsActive] = useState(false);
    const [currentStep, setCurrentStep] = useState(0);
    const [waitingForAction, setWaitingForAction] = useState<string | null>(null);
    const [tourRequestId, setTourRequestId] = useState<string | null>(null);

    const homeCallbacksRef = useRef<HomeCallbacks | null>(null);
    const navigate = useNavigate();
    const location = useLocation();

    // Auto-start tour on first visit to /home
    const hasChecked = useRef(false);
    useEffect(() => {
        if (hasChecked.current) return;
        if (location.pathname === '/home') {
            hasChecked.current = true;
            const done = localStorage.getItem(STORAGE_KEY);
            if (!done) {
                // Small delay to let the page render
                setTimeout(() => setIsActive(true), 1500);
            }
        }
    }, [location.pathname]);

    const registerHomeCallbacks = useCallback((callbacks: HomeCallbacks | null) => {
        homeCallbacksRef.current = callbacks;
    }, []);

    const buildStepContext = useCallback((): TourStepContext => ({
        navigate,
        homeCallbacks: homeCallbacksRef.current,
        getHomeCallbacks: () => homeCallbacksRef.current,
        setTourRequestId,
        tourRequestId,
    }), [navigate, tourRequestId]);

    const runStepEnter = useCallback(async (stepIndex: number) => {
        const step = ONBOARDING_STEPS[stepIndex];
        if (!step) return;

        if (step.onEnter) {
            await step.onEnter(buildStepContext());
        }

        if (step.waitForAction) {
            setWaitingForAction(step.waitForAction);
        } else {
            setWaitingForAction(null);
        }
    }, [buildStepContext]);

    const startTour = useCallback(() => {
        setCurrentStep(0);
        setWaitingForAction(null);
        setTourRequestId(null);
        setIsActive(true);
        // Run step 0's onEnter
        const step = ONBOARDING_STEPS[0];
        if (step?.waitForAction) {
            setWaitingForAction(step.waitForAction);
        }
    }, []);

    const goToStep = useCallback(async (index: number) => {
        if (index < 0 || index >= ONBOARDING_STEPS.length) return;
        setCurrentStep(index);
        await runStepEnter(index);
    }, [runStepEnter]);

    const nextStep = useCallback(() => {
        const next = currentStep + 1;
        if (next >= ONBOARDING_STEPS.length) {
            // Tour complete
            setIsActive(false);
            localStorage.setItem(STORAGE_KEY, 'true');
            return;
        }
        goToStep(next);
    }, [currentStep, goToStep]);

    const skipTour = useCallback(() => {
        setIsActive(false);
        setWaitingForAction(null);
        localStorage.setItem(STORAGE_KEY, 'true');
        // Navigate back to home if not there
        if (location.pathname !== '/home') {
            navigate('/home');
        }
    }, [location.pathname, navigate]);

    const completeTour = useCallback(() => {
        setIsActive(false);
        setWaitingForAction(null);
        localStorage.setItem(STORAGE_KEY, 'true');
    }, []);

    const resolveAction = useCallback((actionId: string) => {
        if (!isActive) return;
        if (waitingForAction === actionId) {
            setWaitingForAction(null);
            // Auto-advance to next step
            const next = currentStep + 1;
            if (next >= ONBOARDING_STEPS.length) {
                completeTour();
                return;
            }
            // Use setTimeout to let the UI settle after the action
            setTimeout(() => goToStep(next), 100);
        }
    }, [isActive, waitingForAction, currentStep, goToStep, completeTour]);

    // Watch for route changes to resolve navigation-based actions
    useEffect(() => {
        if (!isActive || !waitingForAction) return;

        const routeActions: Record<string, string> = {
            'navigate-profile': '/your-profile',
            'navigate-home-from-profile': '/home',
            'navigate-your-requests': '/your-requests',
            'navigate-home-from-requests': '/home',
            'navigate-your-actions': '/your-actions',
            'navigate-home-from-actions': '/home',
        };

        const expectedPath = routeActions[waitingForAction];
        if (expectedPath && location.pathname === expectedPath) {
            resolveAction(waitingForAction);
        }
    }, [location.pathname, isActive, waitingForAction, resolveAction]);

    const value = React.useMemo(() => ({
        isActive,
        currentStep,
        totalSteps: ONBOARDING_STEPS.length,
        waitingForAction,
        tourRequestId,
        startTour,
        nextStep,
        skipTour,
        completeTour,
        resolveAction,
        registerHomeCallbacks,
    }), [
        isActive,
        currentStep,
        waitingForAction,
        tourRequestId,
        startTour,
        nextStep,
        skipTour,
        completeTour,
        resolveAction,
        registerHomeCallbacks,
    ]);

    return (
        <OnboardingContext.Provider value={value}>
            {children}
        </OnboardingContext.Provider>
    );
}
