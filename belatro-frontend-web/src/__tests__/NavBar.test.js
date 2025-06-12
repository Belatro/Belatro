import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import NavBar from '../components/NavBar';
import { BrowserRouter } from 'react-router-dom';

const renderWithRouter = (ui) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

describe('NavBar Component', () => {
  test('shows login/signup when user is not logged in', () => {
    renderWithRouter(<NavBar username={null} setUsername={jest.fn()} />); // rendera navbar bez usernamea/ loged out
    
    expect(screen.getByText(/log in/i)).toBeInTheDocument();
    expect(screen.getByText(/sign up/i)).toBeInTheDocument();//confirma login i sginup da pise dok nema usernamea
  });

  test('shows username and dropdown when user is logged in', () => {
    renderWithRouter(<NavBar username="TestUser" setUsername={jest.fn()} />); //rendera navbas sa usernameon / loged in user
    
    expect(screen.getByText("TestUser")).toBeInTheDocument(); // provjerava da pise username umjesto log in i sign upa
    expect(screen.queryByText(/log in/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/sign up/i)).not.toBeInTheDocument();
  });
});
