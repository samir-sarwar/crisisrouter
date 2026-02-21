import { useEffect } from 'react'

function Login() {
    useEffect(() => {
        // Clear stale session data so Home.tsx flies to saved address on login
        sessionStorage.removeItem('crisisRouter.mapViewport')
        sessionStorage.removeItem('crisisRouter.demoRequests')
        sessionStorage.removeItem('crisisRouter.demoClaims')
        // Redirect to backend Auth0 login via relative URL.
        // In dev: Vite proxy handles this. In prod: Vercel rewrite proxies to Railway.
        // This keeps cookies on the same domain (crisisrouter.xyz).
        window.location.href = '/oauth2/authorization/auth0'
    }, [])

    return null
}

export default Login
