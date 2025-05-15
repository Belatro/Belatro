import React from "react";
import CardComponent from "../components/CardComponent";
import { useNavigate } from "react-router-dom";
import "../App.css";

const HomePage = () => {

    const navigate = useNavigate();

    const handleNavigate = (mode) => {
        navigate("/lobby", { state: { mode } });
    };

    return (
        <div>
            <div className="home-container">
            <br />
            <div className="button-container">
            <button className="game-button" onClick={() => handleNavigate("Ranked")}>
                RANKED MODE
            </button>
            <button className="game-button" onClick={() => handleNavigate("Play Friends")}>
                PLAY FRIENDS
            </button>
            </div>
                <div className="card-container">
                    <CardComponent
                    title={["GRAND MASTER", <br />, <br /> ,"ELO: 3028", <br />, <br />]}
                    text="Igraj te ranked gamemode kako bi ostvarili elo i dizali se na ljestvici!"
                    />
                    <CardComponent
                    text="Napravi te lobby i igraj te sa prijateljima!"
                    />
                </div>
            </div>
        </div>
    );
  };  

export default HomePage;