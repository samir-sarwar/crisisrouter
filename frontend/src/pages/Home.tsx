import React from 'react';
import Map, { Layer, Source } from "react-map-gl/mapbox";
import 'mapbox-gl/dist/mapbox-gl.css';

const Home: React.FC = () => {
    const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN;

    if (!mapboxToken) {
        return <div className="h-screen w-screen flex items-center justify-center bg-black text-white">Error: Mapbox token not found.</div>;
    }

    return (
        <div style={{ height: '100vh', width: '100vw' }}>
            <Map
                initialViewState={{
                    longitude: -79.3832,
                    latitude: 43.6532,
                    zoom: 15.5,
                    pitch: 60,
                    bearing: -17.6,
                }}
                style={{ width: '100%', height: '100%' }}
                mapStyle="mapbox://styles/mapbox/dark-v11"
                mapboxAccessToken={mapboxToken}
            >
                <Source id="mapbox-dem" type="raster-dem" url="mapbox://mapbox.mapbox-terrain-dem-v1" tileSize={512} maxzoom={14} />

                {/* 3D Buildings Layer */}
                <Layer
                    id="3d-buildings"
                    source="composite"
                    source-layer="building"
                    filter={['==', 'extrude', 'true']}
                    type="fill-extrusion"
                    minzoom={15}
                    paint={{
                        'fill-extrusion-color': '#aaa',
                        'fill-extrusion-height': [
                            'interpolate',
                            ['linear'],
                            ['zoom'],
                            15,
                            0,
                            15.05,
                            ['get', 'height']
                        ],
                        'fill-extrusion-base': [
                            'interpolate',
                            ['linear'],
                            ['zoom'],
                            15,
                            0,
                            15.05,
                            ['get', 'min_height']
                        ],
                        'fill-extrusion-opacity': 0.6
                    }}
                />
            </Map>
        </div>
    );
};

export default Home;
