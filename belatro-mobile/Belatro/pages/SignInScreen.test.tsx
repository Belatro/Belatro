import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { Alert } from 'react-native';
import SignInScreen from './SignInScreen';

const mockNavigate = jest.fn();
const mockLogin = jest.fn();

jest.mock('../services/userService', () => ({
  loginUser: jest.fn(),
}));

jest.mock('../context/authContext', () => ({
  useAuth: () => ({ login: mockLogin }),
}));

import { loginUser } from '../services/userService';
const mockedLoginUser = loginUser as jest.MockedFunction<typeof loginUser>;

describe('SignInScreen', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(console, 'error').mockImplementation(() => { });
    jest.spyOn(console, 'warn').mockImplementation(() => { });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders the screen correctly', () => {
    const { getByText } = render(<SignInScreen navigation={{ navigate: mockNavigate }} />);
    expect(getByText('Sign in to Belatro')).toBeTruthy();
    expect(getByText('Username')).toBeTruthy();
    expect(getByText('Password')).toBeTruthy();
  });

  it('shows validation alert when username is empty', () => {
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation(() => { });
    const { getByText } = render(<SignInScreen navigation={{ navigate: mockNavigate }} />);
    fireEvent.press(getByText('Sign in'));
    expect(alertSpy).toHaveBeenCalledWith('Validation', 'Username is required');
    alertSpy.mockRestore();
  });

  it('logs in successfully and navigates', async () => {
    mockedLoginUser.mockResolvedValueOnce({
      data: {
        token: 'fake-token',
        user: { id: 1, username: 'testuser', email: 'test@example.com' },
      },
    } as any);

    const { getByText, getByPlaceholderText } = render(<SignInScreen navigation={{ navigate: mockNavigate }} />);
    fireEvent.changeText(getByPlaceholderText('Username'), 'testuser');
    fireEvent.changeText(getByPlaceholderText('Password'), 'password123');
    fireEvent.press(getByText('Sign in'));

    await waitFor(() => {
      expect(mockedLoginUser).toHaveBeenCalledWith({ username: 'testuser', password: 'password123' });
      expect(mockLogin).toHaveBeenCalledWith(
        {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
          token: 'fake-token',
        },
        'fake-token'
      );
      expect(mockNavigate).toHaveBeenCalledWith('Home', {
        userId: 1,
        username: 'testuser',
        token: 'fake-token',
      });
    });
  });

  it('shows error alert on login failure', async () => {
    mockedLoginUser.mockRejectedValueOnce({ response: { status: 403 } });
    const alertSpy = jest.spyOn(Alert, 'alert').mockImplementation(() => { });

    const { getByText, getByPlaceholderText } = render(<SignInScreen navigation={{ navigate: mockNavigate }} />);
    fireEvent.changeText(getByPlaceholderText('Username'), 'wronguser');
    fireEvent.changeText(getByPlaceholderText('Password'), 'wrongpass');
    fireEvent.press(getByText('Sign in'));

    await waitFor(() => {
      expect(alertSpy).toHaveBeenCalledWith('Error', 'Access denied. Please check your username and password.');
    });

    alertSpy.mockRestore();
  });
});
