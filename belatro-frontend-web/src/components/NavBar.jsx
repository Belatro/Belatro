import React from "react";
import { Navbar, Nav, Container } from "react-bootstrap";
import "../App.css";

const NavBar = () => {
    return (
      <nav className="navbar">
        <div className="nav-left">
          <a href="#" className="nav-link">PLAY</a>
          <a href="#" className="nav-link">LEARN</a>
        </div>
        <div className="nav-center">
          <span className="nav-logo">BELATRO</span>
        </div>
        <div className="nav-right">
          <a href="#" className="nav-link">LOGIN</a>
        </div>
      </nav>
    );
  };
  
  export default NavBar;