import React, { useState } from 'react';
import { Link, useNavigate } from "react-router-dom";
import { fetchAllUsers } from '../services/userService';
import "../App.css";

const LoginPage = ({ setUsername }) => {
  const [usernameInput, setUsernameInput] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");

  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();

    fetchAllUsers().then((users) => {
      const matchedUser = users.find(
        (user) => user.username === usernameInput && user.passwordHashed === password
      );

      if (matchedUser) {
        console.log("Login successful:", matchedUser);
        setLoginError("");
        localStorage.setItem("username", matchedUser.username);
        localStorage.setItem("userId", matchedUser.id);
        setUsername({ username: matchedUser.username, userId: matchedUser.id });
        navigate("/");
      } else {
        setLoginError("Invalid username or password");
      }
    });
  };

  return (
    <div className="lr-container">
      <h2>LOG IN TO BELATRO</h2>
      <br />
      <form className="lr-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          value={usernameInput}
          onChange={(e) => setUsernameInput(e.target.value)}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit" className="lr-button">LOGIN</button>

        {loginError && <p className="error-text">{loginError}</p>}

        <p className="register-text">
          Don't have an account?{" "}
          <Link to="/register" className="lr-link">
            Sign up here!
          </Link>
        </p>
      </form>
    </div>
  );
};

export default LoginPage;
