import React from "react";
import NavBar from "../components/NavBar";
import CardComponent from "../components/CardComponent";
import "../App.css";

const HomePage = () => {
    return (
        <div>
            <NavBar />
            <div className="home-container">
            <br />
            <div className="button-container">
                <button className="game-button">RANKED MODE</button>
                <button className="game-button">QUICK PLAY</button>
                <button className="game-button">PLAY FRIENDS</button>
            </div>
                <div className="card-container">
                    <CardComponent
                    title={["GRAND MASTER", <br />, <br /> ,"ELO: 3028", <br />, <br />]}
                    text="Igraj te ranked gamemode kako bi ostvarili elo i dizali se na ljestvici!"
                    />
                    <CardComponent
                    text="Igraj te quick play ako zelite odigrati brzu igru bez stresa gubljenja elo-a!"
                    />
                    <CardComponent
                    text="Napravi te lobby i odigraj te sa prijateljima!"
                    />
                </div>
            </div>
        </div>
    );
  };  

export default HomePage;