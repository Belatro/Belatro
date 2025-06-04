import React, { useState } from "react";
import CardComponent from "../components/CardComponent";
import LobbyModeModal from "../components/LobbyModeModal";
import { useNavigate } from "react-router-dom";
import { createLobby } from "../services/lobbyService";
import "../App.css";

const HomePage = () => {
  const navigate = useNavigate();
  const [mode, setMode] = useState(null);
  const [showModal, setShowModal] = useState(false);

  const handleGameModeClick = (selectedMode) => {
    setMode(selectedMode);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  const handleHostLobby = async (password) => {
    try {
      const userId = localStorage.getItem("userId");
      const username = localStorage.getItem("username");

      const newLobby = await createLobby({
        gameMode: mode,
        status: "WAITING",
        hostUser: {
          id: userId,
          username: username
        },
        privateLobby: mode === "Play Friends",
        password: mode === "Play Friends" ? password : null
      });

      if (mode === "Play Friends" && password) {
        localStorage.setItem(`lobbyPassword_${newLobby.id}`, password);
      }

      navigate("/lobby", { state: { mode, lobbyId: newLobby.id } });
    } catch (error) {
      console.error("Failed to create lobby:", error);
      alert("Could not create lobby. Please try again.");
    }
  };

  const handleJoinLobby = (lobbyId, password) => {
  setShowModal(false);
  if (!lobbyId) {
    alert("Lobby ID is required to join.");
    return;
  }
  
  if (password) {
    localStorage.setItem(`lobbyPassword_${lobbyId}`, password);
  }

  navigate("/lobby", { state: { mode, lobbyId, password } });
};

  return (
    <div>
      <div className="home-container">
        <br />
        <div className="button-container">
          <button className="game-button" disabled onClick={() => handleGameModeClick("Ranked")}>RANKED MODE</button>
          <button className="game-button" onClick={() => handleGameModeClick("Play Friends")}>PLAY FRIENDS</button>
        </div>

        <div className="card-container">
          <CardComponent
            title={["GRAND MASTER", <br key="1" />, <br key="2" />, "ELO: 3028", <br key="3" />, <br key="4" />]}
            text="Igraj te ranked gamemode kako bi ostvarili elo i dizali se na ljestvici!"
          />
          <CardComponent
            text="Napravi te lobby i igraj te sa prijateljima!"
          />
        </div>
      </div>

      {showModal && (
        <LobbyModeModal
          onClose={handleCloseModal}
          onHost={handleHostLobby}
          onJoin={handleJoinLobby}
          mode={mode}
        />
      )}
    </div>
  );
};

export default HomePage;
