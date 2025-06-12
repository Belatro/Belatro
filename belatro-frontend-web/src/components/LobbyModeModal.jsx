import React, { useState } from 'react';
import '../App.css';

const LobbyModeModal = ({ onClose, onHost, onJoin }) => {
  const [step, setStep] = useState("select"); // select host join
  const [password, setPassword] = useState("");
  const [joinLobbyId, setJoinLobbyId] = useState("");
  const [joinPassword, setJoinPassword] = useState("");

  const handleHostClick = () => setStep("host");
  const handleJoinClick = () => setStep("join");

  const handleHostSubmit = () => {
    onHost(password);
    setPassword("");
  };

  const handleJoinSubmit = () => {
    onJoin(joinLobbyId, joinPassword);
    setJoinLobbyId("");
    setJoinPassword("");
  };

  const renderContent = () => {
    switch (step) {
      case "select":
        return (
          <>
            <h2><strong>SELECT LOBBY OPTION</strong></h2>
            <div className="button-row">
              <button className="lr-button" onClick={handleHostClick}>HOST</button>
              <button className="lr-button" onClick={handleJoinClick}>JOIN</button>
            </div>
          </>
        );

      case "host":
        return (
          <>
            <h2><strong>HOST A LOBBY</strong></h2>
            <label htmlFor="host-password">Set Password:</label>
            <input
              id="host-password"
              type="text"
              placeholder="Enter a password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <div className="button-row">
              <button className="lr-button" onClick={handleHostSubmit}>CREATE</button>
              <button className="lr-button" onClick={() => setStep("select")}>BACK</button>
            </div>
          </>
        );

      case "join":
        return (
          <>
            <h2><strong>JOIN A LOBBY</strong></h2>
            <label htmlFor="join-id">Lobby ID:</label>
            <input
              id="join-id"
              type="text"
              placeholder="Enter lobby ID"
              value={joinLobbyId}
              onChange={(e) => setJoinLobbyId(e.target.value)}
            />
            <label htmlFor="join-password">Password:</label>
            <input
              id="join-password"
              type="text"
              placeholder="Enter password"
              value={joinPassword}
              onChange={(e) => setJoinPassword(e.target.value)}
            />
            <div className="button-row">
              <button className="lr-button" onClick={handleJoinSubmit}>JOIN</button>
              <button className="lr-button" onClick={() => setStep("select")}>BACK</button>
            </div>
          </>
        );

      default:
        return null;
    }
  };

  return (
    <div className="lobby-modal-overlay">
      <div className="lobby-modal-content">
        <button className="close-button" onClick={onClose}>X</button>
        {renderContent()}
      </div>
    </div>
  );
};

export default LobbyModeModal;
