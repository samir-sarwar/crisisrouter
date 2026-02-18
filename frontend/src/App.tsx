import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Home from './pages/Home';
import YourRequests from './pages/YourRequests';
import YourActions from './pages/YourActions';
import YourProfile from './pages/YourProfile';
import OnboardingProvider from './onboarding/OnboardingProvider';
import OnboardingOverlay from './onboarding/OnboardingOverlay';
import './App.css';

function App() {
  return (
    <Router>
      <OnboardingProvider>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/home" element={<Home />} />
          <Route path="/your-requests" element={<YourRequests />} />
          <Route path="/your-actions" element={<YourActions />} />
          <Route path="/your-profile" element={<YourProfile />} />
        </Routes>
        <OnboardingOverlay />
      </OnboardingProvider>
    </Router>
  );
}

export default App;
