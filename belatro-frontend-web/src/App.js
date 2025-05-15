import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProfilePage from './pages/ProfilePage';
import NavBar from './components/NavBar';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

function App() {
  const [loggedInUser, setLoggedInUser] = useState(null);

  useEffect(() => {
    const savedUsername = localStorage.getItem("username");
    const savedUserId = localStorage.getItem("userId");

    if (savedUsername && savedUserId) {
      setLoggedInUser({ username: savedUsername, userId: savedUserId });
    }
  }, []);

  return (
    <Router>
      <NavBar username={loggedInUser?.username} setUsername={setLoggedInUser} />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage setUsername={setLoggedInUser} />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/profile" element={<ProfilePage user={loggedInUser} />} />
      </Routes>
    </Router>
  );
}

export default App;
