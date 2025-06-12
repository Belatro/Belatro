import React from 'react';

const RankInfo = ({ rank, elo }) => {
  return (
    <div className="rank-info-card">
      <h3>RANK INFORMATION</h3>
      <div className="rank-info-content">
        <span>RANK: {rank}</span>
        <span>ELO: {elo}</span>
      </div>
    </div>
  );
};

export default RankInfo;