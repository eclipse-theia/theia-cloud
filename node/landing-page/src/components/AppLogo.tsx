import './AppLogo.css';

import React from 'react';

interface AppLogoProps {
  fileExtension: string;
}
export const AppLogo: React.FC<AppLogoProps> = ({ fileExtension }: AppLogoProps) => (
  <img className='logo' src={`logo.${fileExtension}`}></img>
);
