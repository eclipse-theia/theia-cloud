/* eslint-disable max-len */
import './Header.css';
import { ThemeToggle } from './ThemeToggle';

interface HeaderProps {
  email?: string;
  authenticate?: () => void;
  logoutUrl?: string;
}

// Function to generate Gravatar URL from email using a simple hash
const getGravatarUrl = (email: string, size: number = 40): string => {
  // Simple hash function for demo purposes
  // In production, you might want to use a proper MD5 library
  let hash = 0;
  const str = email.toLowerCase().trim();
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  const hashStr = Math.abs(hash).toString(16).padStart(8, '0');
  return `https://www.gravatar.com/avatar/${hashStr}?s=${size}&d=identicon&r=pg`;
};

export const Header = ({ email, authenticate, logoutUrl }: HeaderProps): JSX.Element => (
  <div className='header'>
    <div className='header__app-name'>
      <h1 className='header__title'>TUM Theia Cloud</h1>
    </div>
    <div className='header__actions'>
      <ThemeToggle />
      {email ? (
        <div className='header__user-info'>
          <img 
            src={getGravatarUrl(email, 40)} 
            alt="User Avatar" 
            className='header__avatar'
          />
          <span className='header__email'>{email}</span>
        </div>
      ) : authenticate ? (
        <button className='header__login-btn' onClick={authenticate}>
          Login
        </button>
      ) : null}
      {logoutUrl && (
        <button 
          className='header__logout-btn'
          onClick={() => window.location.href = logoutUrl}
          data-testid="logoutButton"
        >
          Logout
        </button>
      )}
    </div>
  </div>
);
