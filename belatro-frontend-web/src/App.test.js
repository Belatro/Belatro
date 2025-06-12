import { render, screen } from '@testing-library/react';
import App from './App';

test('renders BELATRO logo', () => {
  render(<App />);
  expect(screen.getByText(/BELATRO/i)).toBeInTheDocument(); // provjerava jeli naslov BELATRO se pojavi kad se App loada
});