import React from 'react';
import "../App.css";

const MatchHistoryTable = ({ matches }) => {
  return (
    <div className="match-history">
      <h2 className="match-history-title">MATCH HISTORY</h2>
      <table className="match-table">
        <thead>
          <tr>
            <th>RESULT</th>
            <th>OUTCOME</th>
            <th>GAMEMODE</th>
            <th>DATE</th>
          </tr>
        </thead>
        <tbody>
          {matches.length === 0 ? (
            <tr>
              <td colSpan="4" style={{ textAlign: "center", color: "gray", padding: "20px" }}>
                No matches found.
              </td>
            </tr>
          ) : (
            matches.map((match, index) => (
              <tr key={index}>
                <td>{match.result}</td>
                <td className={match.yourOutcome === 'WIN' ? 'positive' : 'negative'}>
                  {match.yourOutcome}
                </td>
                <td>{match.gameMode}</td>
                <td>{new Date(match.endTime).toLocaleDateString()}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
};

export default MatchHistoryTable;