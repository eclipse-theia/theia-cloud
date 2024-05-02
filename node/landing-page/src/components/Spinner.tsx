import './Spinner.css';

import React from 'react';

import { AppLogo } from './AppLogo';

export const Spinner: React.FC = () => (
  <div className='spinner-container'>
    <div className='custom-spinner'>
      <AppLogo />
    </div>
  </div>
);
