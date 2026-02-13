import { useState } from 'react'
import '../App.css'

function Login() {
    const [status, setStatus] = useState('IDLE')

    const handleStart = () => {
        setStatus('SCANNING')
        // Redirect to backend Auth0 login — after success the backend redirects to /home
        setTimeout(() => {
            window.location.href = '/oauth2/authorization/auth0'
        }, 1500)
    }

    return (
        <div className="login-layout">
            <div className="scanline"></div>

            <header>
                <div className="mono-label">SYSTEM_INIT_SUCCESS</div>
                <h1>CRISIS_ROUTER</h1>
                <p style={{ color: 'var(--muted-color)', marginTop: '0.5rem' }}>
                    Real-time incident response and routing protocol.
                </p>
            </header>

            <main>
                <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                        <span style={{ color: 'var(--accent-color)' }}>[ NODE_STATUS ]</span>
                        <span>0 ms</span>
                    </div>

                    <p style={{ marginBottom: '1.5rem', lineHeight: '1.4' }}>
                        Welcome to the Crisis Router. This interface serves as the primary gateway for dispatching emergency resources and managing active incidents.
                    </p>

                    <div style={{ padding: '1rem', border: '1px solid var(--border-color)', background: '#000', marginBottom: '1.5rem' }}>
                        <div style={{ color: 'var(--muted-color)', fontSize: '0.75rem', marginBottom: '0.5rem' }}>TERMINAL_OUTPUT</div>
                        <code style={{ background: 'transparent', padding: 0 }}>
                            &gt; INITIALIZING_WEBSOCKET...<br />
                            &gt; CONNECTION_ESTABLISHED<br />
                            &gt; WAITING_FOR_INBOUND_SIGNAL...
                        </code>
                    </div>

                    <button
                        onClick={handleStart}
                        disabled={status === 'SCANNING'}
                    >
                        {status === 'IDLE' ? 'START_NETWORK_SCAN' : 'SCANNING_IN_PROGRESS...'}
                    </button>
                </div>
            </main>

            <footer style={{ marginTop: '4rem', color: 'var(--muted-color)', fontSize: '0.8rem' }}>
                <p>SECURE_ENCRYPTION_ACTIVE // BITS_4096</p>
            </footer>
        </div>
    )
}

export default Login
