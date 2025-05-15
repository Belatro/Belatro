import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "../App.css";
import { IconButton, Menu, MenuItem, Badge, Button } from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import {
  fetchAllFriendsByUserId,
  acceptFriendRequest,
  rejectFriendRequest
} from "../services/userService";

const NavBar = ({ username, setUsername }) => {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [notifAnchorEl, setNotifAnchorEl] = useState(null);
  const [friendRequests, setFriendRequests] = useState([]);
  const navigate = useNavigate();

  const userId = localStorage.getItem("userId");
  console.log("UserId from localStorage:", userId);

useEffect(() => {
  const loadFriendRequests = async () => {
    if (!userId) return;

     try {
      const allFriendships = await fetchAllFriendsByUserId(userId);
      console.log("Fetched friendships:", allFriendships);

      const pendingRequests = allFriendships.filter(
      (f) => f.toUser.id === parseInt(userId) && f.status === "PENDING"
      );
      console.log("Filtered pending requests:", pendingRequests);

      setFriendRequests(pendingRequests);
    } catch (error) {
      console.error("Failed to fetch friend requests:", error);
    }
  };

  loadFriendRequests();
}, [userId]);


  const handleAccept = async (friendshipId) => {
    try {
      await acceptFriendRequest(friendshipId);
      setFriendRequests(prev => prev.filter(f => f.id !== friendshipId));
    } catch (error) {
      console.error("Failed to accept:", error);
    }
  };

  const handleReject = async (friendshipId) => {
    try {
      await rejectFriendRequest(friendshipId);
      setFriendRequests(prev => prev.filter(f => f.id !== friendshipId));
    } catch (error) {
      console.error("Failed to reject:", error);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("username");
    localStorage.removeItem("userId");
    setUsername(null);
    setDropdownOpen(false);
    navigate("/login");
  };

  const toggleDropdown = () => {
    setDropdownOpen((prev) => !prev);
  };

  const handleNotifClick = (event) => {
    setNotifAnchorEl(event.currentTarget);
  };

  const handleNotifClose = () => {
    setNotifAnchorEl(null);
  };

  const isNotifOpen = Boolean(notifAnchorEl);
  const totalNotifications = friendRequests.length;

  return (
    <nav className="navbar">
      <div className="nav-left">
        <Link to="/" className="nav-link">PLAY</Link>
        <a href="#" className="nav-link">LEARN</a>
      </div>

      <div className="nav-center">
        <Link to="/" className="nav-logo">BELATRO</Link>
      </div>

      <div className="nav-right">
        {username ? (
          <div className="dropdown" style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <IconButton color="inherit" onClick={handleNotifClick}>
              <Badge
                badgeContent={totalNotifications}
                color="error"
                invisible={totalNotifications === 0}
              >
                <NotificationsIcon />
              </Badge>
            </IconButton>

            <Menu
              anchorEl={notifAnchorEl}
              open={isNotifOpen}
              onClose={handleNotifClose}
              anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
              transformOrigin={{ vertical: "top", horizontal: "right" }}
            >
              {friendRequests.length > 0 ? (
                friendRequests.map((req, index) => (
                  <MenuItem key={`fr-${index}`} style={{ display: "flex", flexDirection: "column", alignItems: "start", gap: "5px" }}>
                    <span>Friend request from {req.fromUser.username}</span>
                    <div>
                      <Button size="small" onClick={() => handleAccept(req.id)}>Accept</Button>
                      <Button size="small" onClick={() => handleReject(req.id)}>Reject</Button>
                    </div>
                  </MenuItem>
                ))
              ) : (
                <MenuItem onClick={handleNotifClose}>No new friend requests</MenuItem>
              )}
            </Menu>

            <span className="nav-link dropdown-toggle" onClick={toggleDropdown}>
              {username}
            </span>
            {dropdownOpen && (
              <div className="dropdown-menu">
                <Link to="/profile" className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                  PROFILE
                </Link>
                <button className="dropdown-item" onClick={handleLogout}>
                  SIGN OUT
                </button>
              </div>
            )}
          </div>
        ) : (
          <>
            <Link to="/login" className="nav-link">LOG IN</Link>
            <Link to="/register" className="nav-link">SIGN UP</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default NavBar;
