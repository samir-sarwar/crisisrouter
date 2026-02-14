import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Home from './pages/Home';
import YourRequests from './pages/YourRequests';
import YourActions from './pages/YourActions';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/home" element={<Home />} />
        <Route path="/your-requests" element={<YourRequests />} />
        <Route path="/your-actions" element={<YourActions />} />
      </Routes>
    </Router>
  );
}

export default App;
