import React, { useState, useEffect } from 'react';
import { updateUser } from '../services/userService';
import '../App.css';

const EditUserModal = ({ onClose, user, onUpdate }) => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setUsername(user.username || '');
      setEmail(user.email || '');
    }
  }, [user]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

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
      const updatedData = {
        username,
        email,
        passwordHashed: password,
      };

      const updatedUser = await updateUser(user.id, updatedData);
      if (onUpdate) {
        onUpdate(updatedUser);
      }
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.message || 'Failed to update profile.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="edit-profile-modal-overlay">
      <div className="edit-profile-modal-content">
        <button className="close-button" onClick={onClose}>X</button>
        <h2><strong>Edit Profile</strong></h2>
        <form onSubmit={handleSubmit}>
          <label>Username</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <label>Email</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <label>New Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          {error && <p className="error-text">{error}</p>}

          <button type="submit" className="lr-button" disabled={loading}>
            {loading ? 'Saving...' : 'Save Changes'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default EditUserModal;
