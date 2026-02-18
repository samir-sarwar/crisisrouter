import type { HomeCallbacks } from './useOnboarding';
import { generateFakeRequest } from '../utils/demoRequestGenerator';
import type { ActiveRequest } from '../utils/demoRequestGenerator';

export interface TourStep {
    id: string;
    title: string;
    description: string;
    targetSelector: string | null;
    placement: 'top' | 'bottom' | 'left' | 'right' | 'center';
    // Action to run when entering this step
    onEnter?: (ctx: TourStepContext) => Promise<void> | void;
    // Wait for this action ID before allowing next
    waitForAction?: string;
    // Hide the Next button — user must perform the action
    requireAction?: boolean;
    // Delay (ms) after onEnter before showing visuals
    enterDelay?: number;
    // Route this step expects to be on
    expectedRoute?: string;
    // Opacity of the dark overlay (0 = green ring only, 0.75 = default dark overlay)
    overlayOpacity?: number;
}

export interface TourStepContext {
    navigate: (path: string) => void;
    homeCallbacks: HomeCallbacks | null;
    getHomeCallbacks: () => HomeCallbacks | null;
    setTourRequestId: (id: string) => void;
    tourRequestId: string | null;
}

const saveDemoRequests = (requests: ActiveRequest[]) => {
    sessionStorage.setItem('crisisRouter.demoRequests', JSON.stringify(requests));
};

/**
 * Poll for homeCallbacks to become available (Home may still be mounting).
 * Resolves with callbacks, or null after timeout.
 */
function waitForHomeCallbacks(
    getHomeCallbacks: () => HomeCallbacks | null,
    timeoutMs = 3000,
    intervalMs = 200,
): Promise<HomeCallbacks | null> {
    return new Promise((resolve) => {
        const cb = getHomeCallbacks();
        if (cb?.currentUser) {
            resolve(cb);
            return;
        }

        let elapsed = 0;
        const poll = setInterval(() => {
            elapsed += intervalMs;
            const cb = getHomeCallbacks();
            if (cb?.currentUser) {
                clearInterval(poll);
                resolve(cb);
            } else if (elapsed >= timeoutMs) {
                clearInterval(poll);
                resolve(null);
            }
        }, intervalMs);
    });
}

export const ONBOARDING_STEPS: TourStep[] = [
    // Step 0: Welcome
    {
        id: 'welcome',
        title: 'WELCOME TO CRISIS_ROUTER',
        description: 'This quick tour will show you how to report emergencies, respond to nearby crises, and manage your actions. Let\'s get started.',
        targetSelector: null,
        placement: 'center',
        expectedRoute: '/home',
    },

    // Step 1: Profile tab
    {
        id: 'profile-tab',
        title: 'YOUR PROFILE',
        description: 'Start by setting up your profile. Click here to add your phone number, address, and a profile picture.',
        targetSelector: '.nav-link[href="/your-profile"]',
        placement: 'bottom',
        waitForAction: 'navigate-profile',
        requireAction: true,
        expectedRoute: '/home',
    },

    // Step 2: Profile page (read-only, Next button)
    {
        id: 'profile-page',
        title: 'EDIT YOUR INFO',
        description: 'Here you can update your phone number, default address (which centers your map), and profile picture.',
        targetSelector: '.yr-card',
        placement: 'right',
        expectedRoute: '/your-profile',
        enterDelay: 300,
    },

    // Step 3: Back to Map from profile
    {
        id: 'profile-back',
        title: 'BACK TO MAP',
        description: 'Click "Back to Map" to return to the main view.',
        targetSelector: '.yr-back',
        placement: 'bottom',
        waitForAction: 'navigate-home-from-profile',
        requireAction: true,
        expectedRoute: '/your-profile',
    },

    // Step 4: Create request button
    {
        id: 'create-btn',
        title: 'CREATE A CRISIS REQUEST',
        description: 'See an emergency? Click this button to report it and get help from nearby volunteers.',
        targetSelector: '.create-request-btn',
        placement: 'top',
        waitForAction: 'open-request-form',
        requireAction: true,
        expectedRoute: '/home',
        enterDelay: 300,
    },

    // Step 5: Request form
    {
        id: 'request-form',
        title: 'THE REQUEST FORM',
        description: 'Fill out the title, address, severity, and description. A live preview appears on the map as you type. For now, click "Cancel" to continue the tour.',
        targetSelector: '.form-panel',
        placement: 'left',
        waitForAction: 'close-request-form',
        requireAction: true,
        expectedRoute: '/home',
        enterDelay: 400,
    },

    // Step 6: Your Requests tab
    {
        id: 'requests-tab',
        title: 'YOUR REQUESTS',
        description: 'Click here to view all the crisis requests you\'ve created.',
        targetSelector: '.nav-link[href="/your-requests"]',
        placement: 'bottom',
        waitForAction: 'navigate-your-requests',
        requireAction: true,
        expectedRoute: '/home',
    },

    // Step 7: Your Requests page (read-only, Next button)
    {
        id: 'requests-page',
        title: 'MANAGE YOUR REQUESTS',
        description: 'Here you can edit details, mark requests as complete, cancel them, and see who has volunteered.',
        targetSelector: '.yr-section',
        placement: 'right',
        expectedRoute: '/your-requests',
        enterDelay: 300,
    },

    // Step 8: Back to Map from requests
    {
        id: 'requests-back',
        title: 'BACK TO MAP',
        description: 'Click "Back to Map" to return to the main view.',
        targetSelector: '.yr-back',
        placement: 'bottom',
        waitForAction: 'navigate-home-from-requests',
        requireAction: true,
        expectedRoute: '/your-requests',
    },

    // Step 9: Notification spawn
    {
        id: 'notification-spawn',
        title: 'INCOMING ALERT',
        description: 'A nearby crisis has just been reported! See the notification badge? Click the bell to view it.',
        targetSelector: '.notif-bell',
        placement: 'bottom',
        waitForAction: 'open-notification-dropdown',
        requireAction: true,
        expectedRoute: '/home',
        enterDelay: 1500,
        onEnter: async (ctx) => {
            // Wait for Home to mount and register callbacks
            const cb = await waitForHomeCallbacks(ctx.getHomeCallbacks);
            if (!cb || !cb.currentUser) return;

            const fakeRequest = await generateFakeRequest(
                cb.currentUser.latitude,
                cb.currentUser.longitude,
                cb.mapboxToken
            );

            cb.setActiveRequests(prev => {
                const updated = [...prev, fakeRequest];
                saveDemoRequests(updated.filter(r => r.id.toString().startsWith('demo-')));
                return updated;
            });

            cb.setNotifications(prev => [{
                id: fakeRequest.id,
                title: fakeRequest.title,
                address: fakeRequest.address,
                severityLevel: fakeRequest.severityLevel,
                latitude: fakeRequest.latitude,
                longitude: fakeRequest.longitude,
                timestamp: new Date(),
                read: false,
            }, ...prev]);

            ctx.setTourRequestId(fakeRequest.id);
        },
    },

    // Step 10: Click notification
    {
        id: 'click-notification',
        title: 'VIEW THE ALERT',
        description: 'Click this notification to fly to the crisis location on the map.',
        targetSelector: '.notif-bell__item:first-child',
        placement: 'left',
        waitForAction: 'click-notification-item',
        requireAction: true,
        expectedRoute: '/home',
        enterDelay: 300,
    },

    // Step 11: Volunteer on the card
    {
        id: 'volunteer-card',
        title: 'VOLUNTEER TO HELP',
        description: 'The map has flown to the crisis location. Find the request card on the map and click "Volunteer", then confirm with "YES".',
        targetSelector: '.active-card',
        placement: 'bottom',
        waitForAction: 'volunteer-complete',
        requireAction: true,
        expectedRoute: '/home',
        enterDelay: 2500,
        overlayOpacity: 0,
    },

    // Step 12: Your Actions tab
    {
        id: 'actions-tab',
        title: 'YOUR ACTIONS',
        description: 'Now click here to see the request you just volunteered for.',
        targetSelector: '.nav-link[href="/your-actions"]',
        placement: 'bottom',
        waitForAction: 'navigate-your-actions',
        requireAction: true,
        expectedRoute: '/home',
    },

    // Step 13: Your Actions page (read-only, Next button)
    {
        id: 'actions-page',
        title: 'YOUR COMMITMENTS',
        description: 'Here you can see requests you\'ve volunteered for. Click "Requester Info" to see their contact details, or "Drop Out" if you can no longer help.',
        targetSelector: '.ya-card',
        placement: 'right',
        expectedRoute: '/your-actions',
        enterDelay: 300,
    },

    // Step 14: Back to Map from actions
    {
        id: 'actions-back',
        title: 'BACK TO MAP',
        description: 'Click "Back to Map" to return to the main view.',
        targetSelector: '.ya-back',
        placement: 'bottom',
        waitForAction: 'navigate-home-from-actions',
        requireAction: true,
        expectedRoute: '/your-actions',
    },

    // Step 15: Completion
    {
        id: 'completion',
        title: 'YOU\'RE ALL SET',
        description: 'Thank you for touring Crisis Router. Feel free to explore the app on your own — report crises, volunteer for nearby requests, and help your community.',
        targetSelector: null,
        placement: 'center',
        expectedRoute: '/home',
        enterDelay: 500,
        onEnter: (ctx) => {
            ctx.navigate('/home');
        },
    },
];
