import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser } from "../services/userService";
import "../App.css";

const LoginPage = ({ setUsername }) => {
  const [usernameInput, setUsernameInput] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const result = await loginUser({
        username: usernameInput,
        password: password,
      });

      localStorage.setItem("token", result.token);
      localStorage.setItem("userId", result.user.id);
      localStorage.setItem("username", result.user.username);

      setUsername({
        username: result.user.username,
        userId: result.user.id,
      });

      setLoginError("");
      navigate("/");
    } catch (err) {
      setLoginError(err);
    }
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
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
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
