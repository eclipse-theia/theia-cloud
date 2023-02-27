import { useState } from 'react';

import { AppLogo } from './AppLogo';

interface LaunchAppProps {
  appName: string;
  appDefinition: string;
  onStartSession: (appDefinition: string) => void;
  token: string | undefined;
  config: any;
}
export const LaunchApp: React.FC<LaunchAppProps> = ({
  appName,
  appDefinition,
  onStartSession,
  token,
  config
}: LaunchAppProps) => {
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  return (
    <div>
      <AppLogo />
      <p>
        <button
          className='App__try-now-button'
          disabled={!acceptedTerms || (config.useKeycloak && !token)}
          onClick={() => onStartSession(appDefinition)}
        >
          Launch {appName} &nbsp;&nbsp;&rarr;
        </button>
      </p>
      <TermsAndConditions accepted={acceptedTerms} onTermsAccepted={setAcceptedTerms} />
    </div>
  );
};

interface TermsAndConditionsProps {
  accepted: boolean;
  onTermsAccepted: (accepted: boolean) => void;
}
const openTerms = (): boolean => {
  window.open('./terms.html', 'popup', 'width=600,height=600');
  return false;
};
const TermsAndConditions: React.FC<TermsAndConditionsProps> = ({
  accepted,
  onTermsAccepted
}: TermsAndConditionsProps) => (
  <div className='terms-and-conditions'>
    <input id='accept-terms' type='checkbox' checked={accepted} onChange={ev => onTermsAccepted(ev.target.checked)} />
    <label htmlFor='accept-terms'>
      I accept the{' '}
      <a target='popup' href='./terms.html' onClick={openTerms}>
        terms and conditions.
      </a>
    </label>
  </div>
);
