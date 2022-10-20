/* eslint-disable max-len */
import './AppLogo.css';

import React from 'react';

export const AppLogo: React.FC = () => (
  <img className='logo' src={process.env.PUBLIC_URL + '/logo.svg'}></img>
);
