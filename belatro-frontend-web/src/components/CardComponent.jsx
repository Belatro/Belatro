import React from "react";
import { Card, Button } from "react-bootstrap";
import "../App.css";

const CardComponent = ({ title, text }) => {
  return (
    <Card className="custom-card">
      <Card.Body>
        <Card.Title>{title}</Card.Title>
        <Card.Text>{text}</Card.Text>
      </Card.Body>
    </Card>
  );
};

export default CardComponent;
