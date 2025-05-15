import React, { useState } from 'react';
import SkinInventoryModal from './SkinInventoryModal';
import EditUserModal from './EditUserModal';

const UserProfileCard = ({ username, avatar, description, user, onUpdate }) => {
  const [showInventoryModal, setShowInventoryModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);

  return (
    <div className="user-profile-card">
      <img src={avatar} alt="Avatar" className="user-avatar" />
      <div className="user-info">
        <h3>{username}</h3>
        <p>{description}</p>

        <div className="button-row">
          <button
            className="inventory-button long-button"
            onClick={() => setShowInventoryModal(true)}
          >
            INVENTORY
          </button>
          <button
            className="inventory-button short-button"
            onClick={() => setShowEditModal(true)}
          >
            EDIT PROFILE
          </button>
        </div>
      </div>

      {showInventoryModal && (
        <SkinInventoryModal
          onClose={() => setShowInventoryModal(false)}
          userId={user.id}
        />
      )}
      {showEditModal && (
        <EditUserModal
          user={user}
          onClose={() => setShowEditModal(false)}
          onUpdate={(updatedUser) => {
            setShowEditModal(false);
            onUpdate(updatedUser);
          }}
        />
      )}
    </div>
  );
};

export default UserProfileCard;
