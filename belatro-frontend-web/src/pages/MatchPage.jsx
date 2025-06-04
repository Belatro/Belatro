import React, { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import SockJS from "sockjs-client";
import webstomp from "webstomp-client";

const MatchPage = () => {
  const { state } = useLocation();
  const { matchId } = state || {};

  const [publicState, setPublicState] = useState(null);
  const [privateState, setPrivateState] = useState(null);

  const stompClient = useRef(null);

  const token = localStorage.getItem("token");
  const username = localStorage.getItem("username");

  useEffect(() => {
    if (!matchId || !token || !username) {
      console.warn("Missing matchId, token, or username.");
      return;
    }

    console.log("Connecting to matchId:", matchId);

    const socket = new SockJS(`http://localhost:8080/ws?user=${username}`);
    const client = webstomp.over(socket);

    client.connect(
      {
        Authorization: `Bearer ${token}`,
        "X-Player-Name": username,
        "X-Match-ID": matchId,
      },
      () => {
        console.log("WebSocket connected");

        stompClient.current = client;

        client.subscribe(`/topic/games/${matchId}`, (msg) => {
          console.log("Public state received:", msg.body);
          setPublicState(JSON.parse(msg.body));
        });

        client.subscribe(`/user/queue/games/${matchId}`, (msg) => {
          console.log("Private state received:", msg.body);
          setPrivateState(JSON.parse(msg.body));
        });

        client.send(`/app/games/${matchId}/refresh`, {}, "");
      },
      (error) => {
        console.error("WebSocket connection error:", error);
      }
    );

    return () => {
      if (client.connected) {
        client.disconnect(() => {
          console.log("WebSocket disconnected");
        });
      }
    };
  }, [matchId, token, username]);

  const handlePlayCard = (card, declareBela = false) => {
    if (!stompClient.current?.connected) return;

    const payload = {
      playerId: username,
      card,
      declareBela,
    };

    stompClient.current.send(
      `/app/games/${matchId}/play`,
      JSON.stringify(payload),
      {
        Authorization: `Bearer ${token}`,
        "X-Player-Name": username,
        "X-Match-ID": matchId,
      }
    );
  };

  const handleBid = (pass, trump = null) => {
    if (!stompClient.current?.connected) return;

    const payload = {
      playerId: username,
      pass,
      ...(trump && { trump }),
    };

    stompClient.current.send(
      `/app/games/${matchId}/bid`,
      JSON.stringify(payload),
      {
        Authorization: `Bearer ${token}`,
        "X-Player-Name": username,
        "X-Match-ID": matchId,
      }
    );
  };

  return (
    <div className="match-wrapper">
    <div className="match-container">
        <div className="turn-indicator">
        {privateState?.yourTurn ? "Your Turn!" : "Waiting..."}
        </div>

        <div className="table">
        {/* GORE */}
        <div className="player player-top">
            <div className="avatar" />
            <div className="username">Teammate</div>
            <div className="card-count">Cards: ?</div>
        </div>

        {/* LIVO I DESNO */}
        <div className="player player-left">
            <div className="avatar" />
            <div className="username">Opponent A</div>
            <div className="card-count">Cards: ?</div>
        </div>

        <div className="player player-right">
            <div className="avatar" />
            <div className="username">Opponent B</div>
            <div className="card-count">Cards: ?</div>
        </div>

        {/* CENTAR - STOL? */}
        <div className="center-play">
            <div className="played-cards">Played cards will appear here</div>
        </div>

        {/* BOTUNI */}
        <button className="game-button play-button" onClick={() => handlePlayCard(null)}>
            PLAY CARD
        </button>
        <button className="game-button bid-button" onClick={() => handleBid(true)}>
            BID
        </button>

        {/* DONJI IGRAC - LOGED IN IGRAC - MI */}
        <div className="player-hand">
            <div className="player player-bottom">
            <div className="avatar" />
            <div className="username">{username}</div>
            </div>

            <div className="hand-cards">
            {privateState?.hand?.map((card, index) => (
                <div key={index} className="card">
                {card.rank} {card.boja}
                </div>
            ))}
            </div>
        </div>
        </div>
    </div>
    </div>
    );
};

export default MatchPage;
