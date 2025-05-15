import React, { useState } from 'react';
import "../App.css";

const SkinInventoryModal = ({ onClose }) => {
  const [selectedBack, setSelectedBack] = useState("CardBack1.png");
  const [selectedFront, setSelectedFront] = useState("CardFront1.png");

  const cardBacks = ["CardBack1.png", "CardBack2.png", "CardBack3.png"];
  const cardFronts = ["CardFront1.png", "CardFront2.png", "CardFront3.png"];

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <button className="close-button" onClick={onClose}>X</button>
        <h2><strong>SKIN INVENTORY</strong></h2>

        <div className="skin-section">
          <h3><strong>CARD BACKS</strong></h3>
          <div className="skin-grid">
            {cardBacks.map((img) => (
              <img
                key={img}
                src={`/images/backs/${img}`}
                alt={img}
                className={`skin-image ${selectedBack === img ? 'selected-skin' : ''}`}
                onClick={() => setSelectedBack(img)}
              />
            ))}
          </div>
        </div>

        <div className="skin-section">
          <h3><strong>CARD FRONTS</strong></h3>
          <div className="skin-grid">
            {cardFronts.map((img) => (
              <img
                key={img}
                src={`/images/fronts/${img}`}
                alt={img}
                className={`skin-image ${selectedFront === img ? 'selected-skin' : ''}`}
                onClick={() => setSelectedFront(img)}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SkinInventoryModal;
