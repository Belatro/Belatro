import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import UserProfileCard from "../components/UserProfileCard";
import RankInfo from "../components/RankInfo";
import FriendsList from "../components/FriendsList";
import MatchHistoryTable from "../components/MatchHistoryTable";
import { fetchUserById, fetchMatchHistorySummary } from "../services/userService";
import "../App.css";

const ProfilePage = ({ user: initialUser }) => {
  const navigate = useNavigate();
  const [user, setUser] = useState(initialUser || null);
  const [matchHistory, setMatchHistory] = useState([]);

  useEffect(() => {
    const userId = initialUser?.userId || localStorage.getItem("userId");

    if (!userId) {
      navigate("/login");
      return;
    }

    if (!initialUser) {
      fetchUserById(userId)
        .then(setUser)
        .catch((err) => {
          console.error("Failed to fetch user:", err);
          navigate("/login");
        });
    }

    fetchMatchHistorySummary(userId)
      .then((history) => setMatchHistory(history))
      .catch((err) => {
        console.error("Failed to fetch match history:", err);
        setMatchHistory([]);
      });
  }, [navigate, initialUser]);


  if (!user) return <div>Loading...</div>;

  const { username } = user;
  const description = `Bok ja sam ${username} iz Splita i volim igrat Belu! Dodaj te me ako zelite igrati.`;
  const avatar =
    "https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png?20150327203541";
  const rank = "Grandmaster";
  const elo = 3028;

  const handleUserUpdate = (updatedUser) => {
    setUser(updatedUser);
    localStorage.setItem("username", updatedUser.username);
  };

  return (
    <div className="profile-page">
      <div className="left-panel">
        <UserProfileCard
          username={username}
          description={description}
          avatar={avatar}
          user={user}
          onUpdate={handleUserUpdate}
        />
        <RankInfo rank={rank} elo={elo} />
        <FriendsList />
      </div>
      <div className="right-panel">
        <MatchHistoryTable matches={matchHistory} />
      </div>
    </div>
  );
};

export default ProfilePage;
