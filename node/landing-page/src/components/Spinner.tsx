import './Spinner.css';

import React from 'react';

import { AppLogo } from './AppLogo';

interface SpinnerProps {
  logoFileExtension: string;
}

export const Spinner: React.FC<SpinnerProps> = ({ logoFileExtension }: SpinnerProps) => (
  <div className='spinner-container'>
    <div className='custom-spinner'>
      <AppLogo fileExtension={logoFileExtension}/>
    </div>
  </div>
);
