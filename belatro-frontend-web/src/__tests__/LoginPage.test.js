import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LoginPage from '../pages/LoginPage';
import { MemoryRouter } from 'react-router-dom';
import { loginUser } from '../services/userService';


// provjerava jesu li inputi i botuni na stranici, provjerava tocan login i da savea token u local storage, provjerava jeli dode error poruka na failan login


jest.mock('../services/userService'); // mockani api call

describe('LoginPage', () => {
  const mockSetUsername = jest.fn(); 

  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  }); // svaki test se refresha nema stare date

  test('renders input fields and login button', () => {
    render(
      <MemoryRouter>
        <LoginPage setUsername={mockSetUsername} />
      </MemoryRouter>
    );//loada login formu u testu

    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });//provjerava jesu input polja i botuni tu na stranici

  test('submits form and logs in successfully', async () => {
    const fakeResponse = {
      token: 'test-token',
      user: { id: 123, username: 'TestUser' },
    };
    loginUser.mockResolvedValueOnce(fakeResponse); //fakea uspjesan login i vrati fake datu

    render(
      <MemoryRouter>
        <LoginPage setUsername={mockSetUsername} />
      </MemoryRouter>
    );//loada login page

    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: { value: 'TestUser' },
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { value: 'password123' }, // fake valuei upisani u username i password
    });
    fireEvent.click(screen.getByText('LOGIN')); // kliknut login botun

    await waitFor(() => {
      expect(loginUser).toHaveBeenCalledWith({
        username: 'TestUser',
        password: 'password123',
      });
      expect(mockSetUsername).toHaveBeenCalledWith({
        username: 'TestUser',
        userId: 123,
      });
      expect(localStorage.getItem('token')).toBe('test-token');
    });//confirma da je loginUser callan tocno, setUsername dobio usera i token otisao u local storage
  });

  test('shows error on login failure', async () => {
    loginUser.mockRejectedValueOnce('Invalid credentials'); //  fejka failed login da ispise specificnu poruku

    render(
      <MemoryRouter>
        <LoginPage setUsername={mockSetUsername} />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: { value: 'wrong' },
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByText('LOGIN'));//simulirani krivi username i password i pritisnut login botun 

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
    });//confirma da se error message pojavia na ekranu
  });
});
