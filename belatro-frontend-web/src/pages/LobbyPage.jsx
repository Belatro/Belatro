import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  getLobbyById,
  deleteLobby,
  joinLobby,
  switchTeam,
  startMatch
} from "../services/lobbyService";
import "../App.css";

const LobbyPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { mode, lobbyId } = location.state || {};
  const [lobby, setLobby] = useState(null);
  const userId = localStorage.getItem("userId");
  const username = localStorage.getItem("username");

  useEffect(() => {
    let intervalId;

    const fetchLobby = async () => {
      try {
        const data = await getLobbyById(lobbyId);
        setLobby(data);

        const isInLobby =
          data.teamAPlayers?.some(p => p?.id === userId) ||
          data.teamBPlayers?.some(p => p?.id === userId) ||
          data.unassignedPlayers?.some(p => p?.id === userId);

        const isHost = data.hostUser?.id === userId;

        if (!isInLobby && !isHost && userId && lobbyId) {
          await joinLobby({
            lobbyId,
            userId,
            password:
              data.password ||
              localStorage.getItem(`lobbyPassword_${lobbyId}`) ||
              "",
          });
        }

        if (data.status === "CLOSED" && data.matchId) {
          navigate("/match", { state: { matchId: data.matchId } });
        }
      } catch (error) {
        console.error("Failed to fetch or join lobby:", error);
      }
    };

    if (lobbyId) {
      fetchLobby();
      intervalId = setInterval(fetchLobby, 1000);
    }

    return () => clearInterval(intervalId);
  }, [lobbyId]);

  const handleLeaveLobby = async () => {
    if (!lobby || !userId) return;

    const isHost = lobby.hostUser?.id === userId;

    try {
      if (isHost) {
        await deleteLobby(lobby.id);
      } else {
        const leaveRequest = {
          lobbyId,
          userId,
          password:
            lobby.password ||
            localStorage.getItem(`lobbyPassword_${lobbyId}`) ||
            "",
        };
        await joinLobby(leaveRequest);
      }

      navigate("/");
    } catch (error) {
      console.error("Failed to leave lobby:", error);
      alert("Could not leave lobby.");
    }
  };

  const handleJoinSlot = async (team, index) => {
    if (!lobby || !userId || !username) return;

    const password =
      lobby.password ||
      localStorage.getItem(`lobbyPassword_${lobbyId}`) ||
      "";

    const playerInTeamA = lobby.teamAPlayers?.find((p) => p?.id === userId);
    const playerInTeamB = lobby.teamBPlayers?.find((p) => p?.id === userId);
    const isUnassigned = lobby.unassignedPlayers?.some((p) => p?.id === userId);

    const currentTeam = playerInTeamA
      ? "A"
      : playerInTeamB
      ? "B"
      : null;

    const targetTeam = team === "teamAPlayers" ? "A" : "B";
    const selectedPlayer = lobby[team]?.[index];
    const isAlreadyInThisSlot = selectedPlayer?.id === userId;

    if (isAlreadyInThisSlot) return;

    try {
      if (isUnassigned) {
        // ako je user u lobbiju unasigned swapa ga u kliknuti team
        await switchTeam({
          lobbyId,
          userId,
          targetTeam,
        });
      } else if (!currentTeam) {
        // Ako nije u lobbiju uopce (sigurnost iako se nebi trebalo desit)
        await joinLobby({
          lobbyId,
          userId,
          password,
        });
      } else if (currentTeam !== targetTeam) {
        // ako je u drugon timu i zeli swap
        await switchTeam({
          lobbyId,
          userId,
          targetTeam,
        });
      }
      // else: ako je u ispravnon timu onda nemoj nista napravit
    } catch (error) {
      console.error("Failed to join or switch team:", error);
      alert("Failed to join or switch team.");
    }
  };

  const handleStartMatch = async () => {
    if (!lobby || !userId) return;

    const isHost = lobby.hostUser?.id === userId;

    if (!isHost) {
      alert("Only the host can start the match.");
      return;
    }

    try {
      const match = await startMatch(lobbyId);
      if (match?.id) {
        navigate("/match", { state: { matchId: match.id } });
      } else {
        console.warn("Match started, but no ID returned.");
      }
    } catch (error) {
      console.error("Failed to start match:", error);
      alert("Could not start match.");
    }
  };


  const renderSlot = (team, index) => {
    const player = lobby?.[team]?.[index];
    return (
      <button
        key={`${team}-${index}`}
        className="player-button"
        onClick={() => handleJoinSlot(team, index)}
      >
        {player?.username || "JOIN"}
      </button>
    );
  };

  return (
    <div className="lobby-container">
      <div className="lobby-box">
        <div className="team">
          {renderSlot("teamAPlayers", 0)}
          {renderSlot("teamAPlayers", 1)}
        </div>

        <div className="vs-section">
          <h2>VS</h2>
          <button className="start-button" onClick={handleStartMatch}>START</button>
        </div>

        <div className="team">
          {renderSlot("teamBPlayers", 0)}
          {renderSlot("teamBPlayers", 1)}
        </div>
      </div>

      <button className="leave-button" onClick={handleLeaveLobby}>
        LEAVE LOBBY
      </button>

      <div className="lobby-id">LOBBY ID: {lobby?.id || lobbyId}</div>

      <div className="lobby-code">
        CODE:{" "}
        {lobby?.password ||
          localStorage.getItem(`lobbyPassword_${lobbyId}`) ||
          "N/A"}
      </div>
    </div>
  );
};

export default LobbyPage;