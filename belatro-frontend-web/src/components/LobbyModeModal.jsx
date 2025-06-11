import React, { useState, useEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { joinRankedQueue, leaveRankedQueue } from "../services/rankedService";
import SockJS from "sockjs-client";
import webstomp from "webstomp-client";
import '../App.css';

const LobbyModeModal = ({ onClose, onHost, onJoin, mode }) => {
  const [step, setStep] = useState("select");
  const [password, setPassword] = useState("");
  const [joinLobbyId, setJoinLobbyId] = useState("");
  const [joinPassword, setJoinPassword] = useState("");
  const navigate = useNavigate();
  
  useEffect(() => {
    let client;
    const token = localStorage.getItem("token");

    if (mode === "Ranked") {
      setStep("rankedIntro");

      const socket = new SockJS("http://localhost:8080/ws");
      client = webstomp.over(socket);
      client.debug = str => console.log("!!!!!!!!!!!! [STOMP DEBUG]:", str);

      client.connect(
        { Authorization: `Bearer ${token}` },
        () => {
          console.log("!!!!!!!!!!!! STOMP client connected");

          client.subscribe("/user/queue/ranked/status", (msg) => {
            console.log("!!!!!!!!!!!! WebSocket message received:", msg.body);

            const data = JSON.parse(msg.body);

            if (data.state === "IN_QUEUE") {
              console.log("!!!!!!!!!!!!Still in queue...");
            } else if (data.state === "MATCH_FOUND") {
              console.log("!!!!!!!!!!!! Match found! Navigating to match:", data.matchId);
              navigate(`/match/${data.matchId}`);
            } else if (["CANCELLED", "ERROR"].includes(data.state)) {
              console.warn("!!!!!!!!!!!! Queue cancelled or errored. Closing modal.");
              onClose();
            }
          });
        },
        (error) => {
          console.error("!!!!!!!!!!!! STOMP connection failed", error);
        }
      );

    } else {
      setStep("select");
    }

    return () => {
      if (client?.connected) {
        client.disconnect();
      }
    };
  }, [mode]);


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

  const handleJoinRankedQueue = async () => {
    setStep("rankedWaiting");
    try {
      const token = localStorage.getItem("token");
      await joinRankedQueue(token);
    } catch (err) {
      console.error("Failed to join ranked queue:", err);
      setStep("rankedIntro");
      alert("Could not join queue.");
    }
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

      case "rankedIntro":
        return (
          <>
            <h2><strong>RANKED QUEUE</strong></h2>
            <p>Join the queue to compete in ranked Belatro.</p>
            <div className="button-row">
              <button className="lr-button" onClick={handleJoinRankedQueue}>JOIN QUEUE</button>
              <button className="lr-button" onClick={onClose}>CANCEL</button>
            </div>
          </>
        );

      case "rankedWaiting":
        return (
          <>
            <h2><strong>WAITING IN QUEUE</strong></h2>
            <p>Waiting in queue for ranked match...</p>
            <div className="button-row">
              <button className="lr-button" onClick={onClose}>LEAVE QUEUE</button>
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
