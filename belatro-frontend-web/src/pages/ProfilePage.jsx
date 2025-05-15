import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import UserProfileCard from "../components/UserProfileCard";
import RankInfo from "../components/RankInfo";
import FriendsList from "../components/FriendsList";
import MatchHistoryTable from "../components/MatchHistoryTable";
import { fetchUserById } from "../services/userService";
import "../App.css";

const ProfilePage = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const savedUserId = localStorage.getItem("userId");

    if (savedUserId) {
      fetchUserById(savedUserId)
        .then((fullUser) => {
          setUser(fullUser);
        })
        .catch((err) => {
          console.error("Failed to fetch user:", err);
          navigate("/login");
        });
    } else {
      navigate("/login");
    }
  }, [navigate]);

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
    <>
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
          <MatchHistoryTable
            matches={[
              {
                player: username,
                score: "1 - 0",
                opponent: "Meawen",
                eloChange: "+28",
                date: "2025-04-12",
              },
              {
                player: username,
                score: "4 - 6",
                opponent: "MaksM",
                eloChange: "-15",
                date: "2025-04-10",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
              {
                player: username,
                score: "2 - 1",
                opponent: "Tesla",
                eloChange: "+30",
                date: "2025-04-09",
              },
            ]}
          />
        </div>
      </div>
    </>
  );
};

export default ProfilePage;
