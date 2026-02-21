import { useEffect } from 'react'

function Login() {
    useEffect(() => {
        // Clear stale session data so Home.tsx flies to saved address on login
        sessionStorage.removeItem('crisisRouter.mapViewport')
        sessionStorage.removeItem('crisisRouter.demoRequests')
        sessionStorage.removeItem('crisisRouter.demoClaims')
        // Redirect to backend Auth0 login directly (not through Vercel proxy)
        // so the OAuth2 session state is preserved across the redirect flow
        const backendUrl = import.meta.env.VITE_BACKEND_URL || '';
        window.location.href = `${backendUrl}/oauth2/authorization/auth0`
    }, [])

    return null
}

export default Login
