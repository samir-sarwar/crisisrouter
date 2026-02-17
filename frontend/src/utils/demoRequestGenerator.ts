export interface ActiveRequest {
    id: string;
    title: string;
    description: string;
    address: string;
    severityLevel: number;
    latitude: number;
    longitude: number;
    imageUrl: string | null;
    status: string;
    creatorFirstName: string;
    creatorLastName: string;
    type: string;
    customCategory?: string;
}

const TITLES = [
    'Flooding on residential street',
    'Power outage affecting block',
    'Fallen tree blocking road',
    'Medical supplies needed urgently',
    'Building structural damage reported',
    'Water contamination alert',
    'Elderly resident needs evacuation',
    'Gas leak detected in area',
    'Road collapse after heavy rain',
    'Fire damage - shelter needed',
];

const DESCRIPTIONS = [
    'Multiple residents affected. Immediate assistance required.',
    'Area has been without power for several hours. Vulnerable residents need support.',
    'Road is completely blocked. Emergency vehicles cannot pass.',
    'Local clinic running low on critical supplies. Volunteers needed for transport.',
    'Structural assessment needed. Residents evacuated as precaution.',
    'Water testing shows contamination. Bottled water distribution needed.',
    'Mobility-impaired resident on upper floor needs help getting to safety.',
    'Utility company notified but response delayed. Area needs to be cordoned off.',
    'Section of road has caved in. Traffic rerouting assistance needed.',
    'Family displaced. Need temporary shelter and basic necessities.',
];

const CATEGORIES = ['Medical', 'Shelter', 'Food & Water', 'Transport', 'Rescue'];

const FIRST_NAMES = ['Sarah', 'James', 'Priya', 'Marcus', 'Elena', 'David', 'Fatima', 'Chen', 'Olga', 'Amir'];
const LAST_NAMES = ['Chen', 'Williams', 'Patel', 'Johnson', 'Rodriguez', 'Kim', 'Hassan', 'Murphy', 'Singh', 'Brown'];

function pick<T>(arr: T[]): T {
    return arr[Math.floor(Math.random() * arr.length)];
}

function randomLocationWithin(lat: number, lng: number, radiusKm: number) {
    const angle = Math.random() * 2 * Math.PI;
    const distance = Math.random() * radiusKm * 1000; // meters
    const offsetLat = (distance / 111320) * Math.cos(angle);
    const offsetLng = (distance / (111320 * Math.cos(lat * Math.PI / 180))) * Math.sin(angle);
    return {
        latitude: lat + offsetLat,
        longitude: lng + offsetLng,
    };
}

export async function generateFakeRequest(
    userLat: number,
    userLng: number,
    mapboxToken: string
): Promise<ActiveRequest> {
    const loc = randomLocationWithin(userLat, userLng, 3);

    let address = `Near ${loc.latitude.toFixed(4)}, ${loc.longitude.toFixed(4)}`;
    try {
        const res = await fetch(
            `https://api.mapbox.com/geocoding/v5/mapbox.places/${loc.longitude},${loc.latitude}.json?access_token=${mapboxToken}&limit=1`
        );
        const data = await res.json();
        if (data.features && data.features.length > 0) {
            address = data.features[0].place_name;
        }
    } catch {
        // Use fallback address
    }

    return {
        id: `demo-${crypto.randomUUID()}`,
        title: pick(TITLES),
        description: pick(DESCRIPTIONS),
        address,
        severityLevel: pick([1, 2, 3, 4]),
        latitude: loc.latitude,
        longitude: loc.longitude,
        imageUrl: null,
        status: 'OPEN',
        creatorFirstName: pick(FIRST_NAMES),
        creatorLastName: pick(LAST_NAMES),
        type: pick(CATEGORIES),
    };
}
