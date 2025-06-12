import React from 'react';
import { render, screen } from '@testing-library/react';
import MatchHistoryTable from '../components/MatchHistoryTable';

// provjerava sve columne u tablici, fallback poruka ako je prazan, oba fake matcha stingla, win-positiv i loss-negative, provjerava gamemode text

describe('MatchHistoryTable', () => {
  const mockMatches = [ //mockavanje date
    {
      result: 'Team A: 82 - Team B: 64',
      yourOutcome: 'WIN',
      gameMode: 'RANKED',
      endTime: new Date().toISOString(),
    },
    {
      result: 'Team A: 50 - Team B: 90',
      yourOutcome: 'LOSE',
      gameMode: 'FRIENDLY',
      endTime: new Date().toISOString(),
    }
  ];

  test('renders table headers', () => {
    render(<MatchHistoryTable matches={[]} />);
    expect(screen.getByText('RESULT')).toBeInTheDocument();
    expect(screen.getByText('OUTCOME')).toBeInTheDocument();
    expect(screen.getByText('GAMEMODE')).toBeInTheDocument();
    expect(screen.getByText('DATE')).toBeInTheDocument();
  }); // gleda jesu li svi columni renderani

  test('shows "No matches found" if match list is empty', () => {
    render(<MatchHistoryTable matches={[]} />);
    expect(screen.getByText(/No matches found/i)).toBeInTheDocument();
  }); // testira jeli fallback poruka kad je prazan match history dobro ispisan

  test('renders match rows correctly', () => {
    render(<MatchHistoryTable matches={mockMatches} />);
    
    expect(screen.getByText('Team A: 82 - Team B: 64')).toBeInTheDocument();
    expect(screen.getByText('WIN')).toHaveClass('positive');
    
    expect(screen.getByText('Team A: 50 - Team B: 90')).toBeInTheDocument();
    expect(screen.getByText('LOSE')).toHaveClass('negative');
    
    expect(screen.getByText('RANKED')).toBeInTheDocument();
    expect(screen.getByText('FRIENDLY')).toBeInTheDocument();
  }); // confirma da su oba fake matcha se pokazala, provjerava jeli win positive i lose negative, provjerava da je pravi gamemode text renderan
});
