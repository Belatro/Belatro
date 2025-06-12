import React, { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import SockJS from "sockjs-client";
import webstomp from "webstomp-client";

const MatchPage = () => {
  const { state } = useLocation();
  const { matchId } = state || {};

  const [publicState, setPublicState] = useState(null);
  const [privateState, setPrivateState] = useState(null);
  const [teammate, setTeammate] = useState(null);
  const [opponentLeft, setOpponentLeft] = useState(null);
  const [opponentRight, setOpponentRight] = useState(null);
  const [selectedCard, setSelectedCard] = useState(null);
  const [visibleTrickPlays, setVisibleTrickPlays] = useState([]);
  const [connectionStatus, setConnectionStatus] = useState("connecting");

  const stompClient = useRef(null);
  const clearTrickTimeout = useRef(null);
  const reconnectAttempts = useRef(0);
  const reconnectTimeout = useRef(null);

  const token = localStorage.getItem("token");
  const username = localStorage.getItem("username");
  const userId = localStorage.getItem("userId");

  const getCardImagePath = (card) => {
    const suitMap = { HERC: "herc", KARA: "kara", PIK: "pik", TREF: "tref" };
    const rankMap = {
      AS: "As", BABA: "Baba", DECKO: "Decko", KRALJ: "Kralj",
      SEDMICA: "7", OSMICA: "8", DEVETKA: "9", DESETKA: "10"
    };
    const suit = suitMap[card.boja?.toUpperCase()];
    const rank = rankMap[card.rank?.toUpperCase()];
    return suit && rank ? `/cards/Fronts/${suit}/${suit}${rank}.png` : null;
  };

  const getTrumpIcon = () => {
    const trump = publicState?.bids?.find(b => b.selectedTrump)?.selectedTrump;
    const iconMap = {
      KARA: "/images/icons/karaIcon.png",
      HERC: "/images/icons/hercIcon.png",
      TREF: "/images/icons/trefIcon.png",
      PIK: "/images/icons/pikIcon.png"
    };
    return trump ? <img src={iconMap[trump]} alt={trump} className="trump-icon" /> : null;
  };

  const connectWebSocket = () => {
    setConnectionStatus("connecting");

    const sock = new SockJS(`${process.env.REACT_APP_API_URL}/ws?user=${username}`);
    const client = webstomp.over(sock);

    client.connect(
      {
        Authorization: `Bearer ${token}`,
        "X-Player-Name": username,
        "X-Match-ID": matchId,
      },
      () => {
        setConnectionStatus("open");
        reconnectAttempts.current = 0;
        stompClient.current = client;

        client.subscribe(`/topic/games/${matchId}`, (msg) => {
          const state = JSON.parse(msg.body);
          setPublicState(state);

          const { currentTrick, seatingOrder, gameState } = state;

          if (gameState === "BIDDING") setVisibleTrickPlays([]);

          if (seatingOrder) {
            const meIndex = seatingOrder.findIndex(p => p.id === username);
            if (meIndex >= 0) {
              const getAt = offset => seatingOrder[(meIndex + offset) % seatingOrder.length] || null;
              setTeammate(getAt(2));
              setOpponentLeft(getAt(3));
              setOpponentRight(getAt(1));
            }
          }

          if (gameState !== "BIDDING" && currentTrick?.plays && seatingOrder) {
            const plays = seatingOrder
              .map(p => ({ playerId: p.id, card: currentTrick.plays[p.id] }))
              .filter(p => p.card);
            setVisibleTrickPlays(plays);
          } else {
            setVisibleTrickPlays([]);
          }
        });

        client.subscribe(`/user/queue/games/${matchId}`, (msg) => {
          setPrivateState(JSON.parse(msg.body));
        });

        client.send(`/app/games/${matchId}/refresh`, {}, "");
      },
      (err) => {
        console.error("WebSocket error", err);
        setConnectionStatus("error");
      }
    );

    sock.onclose = () => {
      setConnectionStatus("reconnecting");
      const delay = Math.min(1000 * 2 ** reconnectAttempts.current, 30000);
      reconnectTimeout.current = setTimeout(() => {
        reconnectAttempts.current += 1;
        connectWebSocket();
      }, delay);
    };

    return () => {
      if (client.connected) client.disconnect();
      if (reconnectTimeout.current) clearTimeout(reconnectTimeout.current);
    };
  };

  useEffect(() => {
    if (!matchId || !token || !username || !userId) return;
    const cleanup = connectWebSocket();
    return cleanup;
  }, [matchId, token, username, userId]);

  useEffect(() => {
    if (publicState?.gameState === "BIDDING") setVisibleTrickPlays([]);
  }, [publicState?.gameState]);

  const handlePlayCard = (card, declareBela = false) => {
    if (!stompClient.current?.connected || !card) return;
    stompClient.current.send(
      `/app/games/${matchId}/play`,
      JSON.stringify({ playerId: username, card, declareBela }),
      { Authorization: `Bearer ${token}` }
    );
  };

  const handleBid = (pass, trump = null) => {
    if (!stompClient.current?.connected) return;
    stompClient.current.send(
      `/app/games/${matchId}/bid`,
      JSON.stringify({ playerId: username, pass, ...(trump && { trump }) }),
      { Authorization: `Bearer ${token}` }
    );
  };

  const handleChallenge = () => {
    if (!stompClient.current?.connected) return;
    stompClient.current.send(
      `/app/games/${matchId}/challenge`,
      JSON.stringify({ playerId: username }),
      { Authorization: `Bearer ${token}` }
    );
  };

  const getYourTeam = () => {
    if (!publicState || !username) return null;
    const isInTeamA = publicState.teamA?.some(p => p.id === username);
    const isInTeamB = publicState.teamB?.some(p => p.id === username);
    if (isInTeamA) return <span style={{ color: "limegreen" }}>You are on Team A</span>;
    if (isInTeamB) return <span style={{ color: "red" }}>You are on Team B</span>;
    return null;
  };


  return (
    <div className="match-wrapper">
      {connectionStatus === "reconnecting" && (
        <div className="reconnecting-banner">Reconnecting...</div>
      )}
      {connectionStatus === "error" && (
        <div className="error-banner">Connection lost. Please refresh.</div>
      )}

      <div className="match-container">

        {/* trump ikonica/boja - gore desno*/}
        <div className="trump-indicator">
          {getTrumpIcon()}
        </div>

        {/* Turn indikator */}
        <div className={`turn-indicator ${privateState?.yourTurn ? "your-turn" : "waiting"}`}>
          {privateState?.yourTurn ? "Your Turn!" : "Waiting..."}
        </div>
        
        {/* tim label i scoreboard */}
        {publicState?.teamAScore !== undefined && publicState?.teamBScore !== undefined && (
          <div className="match-top-bar">
            <div className="team-label">{getYourTeam()}</div>
            <div className="scoreboard">
              <span style={{ color: "limegreen" }}>Team A: {publicState.teamAScore}</span>
              <span style={{ color: "red" }}>Team B: {publicState.teamBScore}</span>
            </div>
          </div>
        )}

        <div className="table">
          {/* gornji igrac - teammate */}
          <div className="player player-top">
            <div className="avatar" />
            <div className="username">{teammate?.username}</div>
            <div className="card-back-stack">
              {[...Array(teammate?.cardsLeft || 0)].map((_, i) => (
                <img key={i} src="/cards/Backs/CardBack1.png" alt="Back" className="stacked-card" />
              ))}
            </div>
          </div>

          {/* ljevi igrac - neprijatelj */}
          <div className="player player-left">
            <div className="avatar" />
            <div className="username">{opponentLeft?.username}</div>
            <div className="card-back-stack">
              {[...Array(opponentLeft?.cardsLeft || 0)].map((_, i) => (
                <img key={i} src="/cards/Backs/CardBack1.png" alt="Back" className="stacked-card" />
              ))}
            </div>
          </div>

          {/* desni igrac - neprijatelj */}
          <div className="player player-right">
            <div className="avatar" />
            <div className="username">{opponentRight?.username}</div>
            <div className="card-back-stack">
              {[...Array(opponentRight?.cardsLeft || 0)].map((_, i) => (
                <img key={i} src="/cards/Backs/CardBack1.png" alt="Back" className="stacked-card" />
              ))}
            </div>
          </div>

          {/* stol / centar odigrane karte */}
          <div className="played-cards">
            {visibleTrickPlays.map((play, idx) => {
              const src = getCardImagePath(play.card);
              return (
                <img
                  key={idx}
                  src={src}
                  alt={`${play.card.rank} ${play.card.boja}`}
                  className="played-card-image"
                />
              );
            })}
          </div>

          <button
            className="game-button play-button"
            onClick={() => handlePlayCard(selectedCard)}
            disabled={!privateState?.yourTurn}
          >
            PLAY CARD
          </button>

          <button
            className="game-button challenge-button"
            onClick={() => handleChallenge()}
          >
            CHALLENGE
          </button>
          
          {/* bidding botuni */}
          {privateState?.yourTurn && publicState?.gameState === "BIDDING" && (
            <div className="center-bid-buttons">
              <button onClick={() => handleBid(false, "KARA")}>
                <img src="/images/icons/karaIcon.png" alt="KARA" />
              </button>
              <button onClick={() => handleBid(false, "HERC")}>
                <img src="/images/icons/hercIcon.png" alt="HERC" />
              </button>
              <button onClick={() => handleBid(false, "TREF")}>
                <img src="/images/icons/trefIcon.png" alt="TREF" />
              </button>
              <button onClick={() => handleBid(false, "PIK")}>
                <img src="/images/icons/pikIcon.png" alt="PIK" />
              </button>
              {publicState.bids?.length < 3 && (
                <button className="skip-button" onClick={() => handleBid(true)}>SKIP</button>
              )}
            </div>
          )}

          {/* donji igrac ruka */}
          <div className="player-hand">
            <div className="player player-bottom">
              <div className="avatar" />
              <div className="username">{username}</div>
            </div>
            <div className="hand-cards">
              {privateState?.hand?.map((card, idx) => {
                const src = getCardImagePath(card);
                const isSelected =
                  selectedCard?.boja === card.boja &&
                  selectedCard?.rank === card.rank;

                return (
                  <img
                    key={idx}
                    src={src}
                    alt={`${card.rank} ${card.boja}`}
                    className={`hand-card-image ${isSelected ? "selected" : ""}`}
                    onClick={() => setSelectedCard(card)}
                  />
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MatchPage;
