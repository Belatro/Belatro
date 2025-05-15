import React from 'react';
import "../App.css";

const MatchHistoryTable = ({ matches }) => {
    return (
        <div className="match-history">
            <h2 className="match-history-title">MATCH HISTORY</h2>
            <table className="match-table">
                <thead>
                    <tr>
                        <th>PLAYER</th>
                        <th>SCORE</th>
                        <th>OPPONENT</th>
                        <th>ELO CHANGE</th>
                        <th>DATE</th>
                    </tr>
                </thead>
                <tbody>
                    {(matches || []).map((match, index) => (
                        <tr key={index}>
                            <td>{match.player}</td>
                            <td>{match.score}</td>
                            <td>{match.opponent}</td>
                            <td className={match.eloChange.startsWith('+') ? 'positive' : 'negative'}>{match.eloChange}</td> 

                            <td>{match.date}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
};

export default MatchHistoryTable;
