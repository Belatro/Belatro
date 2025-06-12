import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { registerUser } from "../services/userService";
import "../App.css";

function RegisterPage() {
  const navigate = useNavigate();

  const [username, setUsername]       = useState("");
  const [email, setEmail]             = useState("");
  const [password, setPassword]       = useState("");
  const [confirmPassword, setConfirm] = useState("");
  const [error, setError]             = useState("");
  const [loading, setLoading]         = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    if (!username || !email || !password) {
      setError("All fields are required.");
      return;
    }

    if (password.length < 6 || password.length > 16) {
      setError("Password must be between 6 and 16 characters.");
      return;
    }

    if (!/\d/.test(password)) {
      setError("Password must contain at least one number.");
      return;
    }

    setLoading(true);
    try {
      const result = await registerUser({
        username,
        email,
        passwordHashed: password,
      });

        localStorage.setItem("token", result.token);
        localStorage.setItem("userId", result.user.id);
        localStorage.setItem("username", result.user.username);
        
        navigate("/home");

    } catch (err) {
      console.error("Registration error:", err);
      const msg =
        err.response?.data?.message ||
        "Registration failed. Please try again.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="lr-container">
      <h2 className="lr-title">SIGN UP FOR BELATRO</h2>
      <br />
      <form className="lr-form" onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
        />
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Confirm Password"
          value={confirmPassword}
          onChange={(e) => setConfirm(e.target.value)}
          required
        />

        {error && <p className="error-text">{error}</p>}

        <button type="submit" className="lr-button" disabled={loading}>
          {loading ? "REGISTERING…" : "REGISTER"}
        </button>

        <p className="register-text">
          Already have an account?{" "}
          <Link to="/login" className="lr-link">
            Click here to login!
          </Link>
        </p>
      </form>
    </div>
  );
}

export default RegisterPage;
