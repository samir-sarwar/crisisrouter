import { useEffect } from 'react'

function Login() {
    useEffect(() => {
        // Clear stale session data so Home.tsx flies to saved address on login
        sessionStorage.removeItem('crisisRouter.mapViewport')
        sessionStorage.removeItem('crisisRouter.demoRequests')
        sessionStorage.removeItem('crisisRouter.demoClaims')
        // Redirect to backend Auth0 login — after success the backend redirects to /home
        window.location.href = '/oauth2/authorization/auth0'
    }, [])

    return null
}

export default Login
