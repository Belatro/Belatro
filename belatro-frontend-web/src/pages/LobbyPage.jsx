import React from "react";
import { useLocation } from "react-router-dom";

const LobbyPage = () => {
    const location = useLocation();
    const mode = location.state?.mode || "Unknown";

    return (
        <div>
            <div className="lobby-container">
                <h2>Game Lobby - {mode}</h2>
                <p>Waiting for players...</p>
            </div>
        </div>
    );
};

export default LobbyPage;