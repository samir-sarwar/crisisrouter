import { createContext, useContext } from 'react';
import type { ActiveRequest } from '../utils/demoRequestGenerator';
import type { NearbyNotification } from '../components/NotificationBell';

export interface HomeCallbacks {
    setActiveRequests: React.Dispatch<React.SetStateAction<ActiveRequest[]>>;
    setNotifications: React.Dispatch<React.SetStateAction<NearbyNotification[]>>;
    setIsCreatingRequest: React.Dispatch<React.SetStateAction<boolean>>;
    mapRef: React.RefObject<any>;
    currentUser: { id: string; firstName: string; lastName: string; latitude: number; longitude: number } | null;
    mapboxToken: string;
}

export interface OnboardingContextValue {
    isActive: boolean;
    currentStep: number;
    totalSteps: number;
    waitingForAction: string | null;
    tourRequestId: string | null;
    startTour: () => void;
    nextStep: () => void;
    skipTour: () => void;
    completeTour: () => void;
    resolveAction: (actionId: string) => void;
    registerHomeCallbacks: (callbacks: HomeCallbacks | null) => void;
}

export const OnboardingContext = createContext<OnboardingContextValue>({
    isActive: false,
    currentStep: 0,
    totalSteps: 0,
    waitingForAction: null,
    tourRequestId: null,
    startTour: () => {},
    nextStep: () => {},
    skipTour: () => {},
    completeTour: () => {},
    resolveAction: () => {},
    registerHomeCallbacks: () => {},
});

export function useOnboarding() {
    return useContext(OnboardingContext);
}
