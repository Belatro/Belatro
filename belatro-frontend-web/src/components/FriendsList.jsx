import React, { useState, useEffect } from "react";
import {
  fetchAllUsers,
  fetchAllFriendsByUserId,
  fetchFriendRequests,
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  deleteFriendship,
} from "../services/userService";

const FriendsList = () => {
  const [activeTab, setActiveTab] = useState("myFriends");
  const [query, setQuery] = useState("");
  const [allUsers, setAllUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [myFriends, setMyFriends] = useState([]);
  const [friendRequests, setFriendRequests] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const userId = localStorage.getItem("userId");

  useEffect(() => {
    if (userId && activeTab === "myFriends") {
      const loadFriends = async () => {
        try {
          const friendsData = await fetchAllFriendsByUserId(userId);
          const acceptedFriends = friendsData.filter(
            (friendship) => friendship.status === "ACCEPTED"
          );
          setMyFriends(acceptedFriends);
          setFilteredUsers(acceptedFriends);
        } catch (error) {
          console.error("Error fetching friends:", error);
        }
      };

      loadFriends();
    }
  }, [activeTab, userId]);

  useEffect(() => {
    const lowerQuery = query.toLowerCase();

    if (activeTab === "findUsers") {
      const loadUsers = async () => {
        try {
          const users = await fetchAllUsers();
          const usersExcludingSelf = users.filter((user) => user.id !== userId);
          setAllUsers(usersExcludingSelf);

          const filtered = usersExcludingSelf.filter((user) =>
            user.username.toLowerCase().includes(lowerQuery)
          );
          setFilteredUsers(filtered);
        } catch (error) {
          console.error("Error fetching users:", error);
        }
      };

      loadUsers();
    } else if (activeTab === "friendRequests") {
      const loadFriendRequests = async () => {
        try {
          const requests = await fetchFriendRequests(userId);
            const filtered = requests.filter((req) => req.fromUser?.id !== userId);
            setFriendRequests(filtered);
        } catch (error) {
          console.error("Error fetching friend requests:", error);
        }
      };

      loadFriendRequests();
    } else {
      const filtered = myFriends.filter((friendship) => {
        if (!friendship || !friendship.fromUser || !friendship.toUser) return false;
        const user =
          friendship.fromUser.id === userId ? friendship.toUser : friendship.fromUser;
        return user.username.toLowerCase().includes(lowerQuery);
      });
      setFilteredUsers(filtered);
    }
  }, [activeTab, query, myFriends, userId]);

  const handleUserClick = (userId) => {
    setSelectedUserId((prev) => (prev === userId ? null : userId));
  };

  const handleAddFriend = async (user) => {
    try {
      await sendFriendRequest(userId, user.id);
      setSelectedUserId(null);
    } catch (error) {
      setError(`Failed to send friend request to ${user.username}: ${error}`);
    }
  };

  const handleDeleteFriend = async (friendshipId) => {
    try {
      await deleteFriendship(friendshipId);
      setMyFriends((prev) => prev.filter((f) => f.id !== friendshipId));
      setSelectedUserId(null);
    } catch (error) {
      setError("Failed to delete friend");
    }
  };

  const handleAcceptRequest = async (friendshipId) => {
    try {
      await acceptFriendRequest(friendshipId);
      setFriendRequests((prev) => prev.filter((req) => req.id !== friendshipId));
    } catch (error) {
      setError("Failed to accept friend request");
    }
  };

  const handleRejectRequest = async (friendshipId) => {
    try {
      await rejectFriendRequest(friendshipId);
      setFriendRequests((prev) => prev.filter((req) => req.id !== friendshipId));
    } catch (error) {
      setError("Failed to reject friend request");
    }
  };

  return (
    <div className="friends-list">
      <div className="tabs">
        <button
          className={`tab-button ${activeTab === "myFriends" ? "active" : ""}`}
          onClick={() => setActiveTab("myFriends")}
        >
          MY FRIENDS
        </button>
        <button
          className={`tab-button ${activeTab === "findUsers" ? "active" : ""}`}
          onClick={() => setActiveTab("findUsers")}
        >
          FIND USERS
        </button>
        <button
          className={`tab-button ${activeTab === "friendRequests" ? "active" : ""}`}
          onClick={() => setActiveTab("friendRequests")}
        >
          FRIEND REQUESTS
        </button>
      </div>

      <div className="friends-search-container">
        <input
          type="text"
          placeholder={activeTab === "myFriends" ? "SEARCH FRIENDS" : "SEARCH USERS"}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="friends-search-input"
        />
      </div>

      {success && <div className="success-message">{success}</div>}
      {error && <div className="error-message">{error}</div>}

      <ul>
        {activeTab === "friendRequests" ? (
          friendRequests.map((request) => (
            <li key={request.id} className="friend-item">
              <img
                src={
                  request.fromUser.avatar ||
                  "https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png"
                }
                alt={request.fromUser.username}
                className="friend-avatar"
              />
              <div style={{ display: "flex", flexDirection: "column" }}>
                <span>{request.fromUser.username}</span>
                <div className="friend-request-actions">
                  <button
                    className="accept-button"
                    onClick={() => handleAcceptRequest(request.id)}
                  >
                    ✔
                  </button>
                  <button
                    className="reject-button"
                    onClick={() => handleRejectRequest(request.id)}
                  >
                    ✘
                  </button>
                </div>
              </div>
            </li>
          ))
        ) : (
          filteredUsers.map((friendshipOrUser) => {
            if (activeTab === "myFriends") {
              if (
                !friendshipOrUser ||
                !friendshipOrUser.fromUser ||
                !friendshipOrUser.toUser
              ) {
                console.warn("Invalid friendship data:", friendshipOrUser);
                return null;
              }

              const user =
                friendshipOrUser.fromUser.id === userId
                  ? friendshipOrUser.toUser
                  : friendshipOrUser.fromUser;

              if (!user) {
                console.warn("Friend user undefined in friendship:", friendshipOrUser);
                return null;
              }

              return (
                <li
                  key={friendshipOrUser.id || user.id}
                  className="friend-item"
                  onClick={() => handleUserClick(user.id)}
                  style={{ cursor: "pointer" }}
                >
                  <img
                    src={
                      user.avatar ||
                      "https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png"
                    }
                    alt={user.name || user.username || "friend"}
                    className="friend-avatar"
                  />
                  <div style={{ display: "flex", flexDirection: "column" }}>
                    <span>{user.username || "Unknown User"}</span>
                    {selectedUserId === user.id && (
                      <button
                        className="add-delete-friend-button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteFriend(friendshipOrUser.id);
                        }}
                      >
                        DELETE
                      </button>
                    )}
                  </div>
                </li>
              );
            } else {
              const user = friendshipOrUser;
              return (
                <li
                  key={user.id}
                  className="friend-item"
                  onClick={() => handleUserClick(user.id)}
                  style={{ cursor: "pointer" }}
                >
                  <img
                    src={
                      user.avatar ||
                      "https://upload.wikimedia.org/wikipedia/commons/7/7c/Profile_avatar_placeholder_large.png"
                    }
                    alt={user.name || user.username}
                    className="friend-avatar"
                  />
                  <div style={{ display: "flex", flexDirection: "column" }}>
                    <span>{user.username}</span>
                    {activeTab === "findUsers" && selectedUserId === user.id && (
                      <button
                        className="add-delete-friend-button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAddFriend(user);
                        }}
                      >
                        ADD FRIEND
                      </button>
                    )}
                  </div>
                </li>
              );
            }
          })
        )}
      </ul>
    </div>
  );
};

export default FriendsList;
