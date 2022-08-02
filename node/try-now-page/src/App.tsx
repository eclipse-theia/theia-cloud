import './App.css';

import { getTheiaCloudConfig, startSession } from '@theia-cloud/common';
import React, { useState } from 'react';

import { Spinner } from './components/Spinner';
import { AppLogo } from './components/AppLogo';

function App(): JSX.Element {
  const [config] = useState(() => getTheiaCloudConfig());
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  if (config === undefined) {
    return (
      <div className='App'>
        <strong>FATAL: Theia.Cloud configuration could not be found.</strong>
      </div>
    );
  }

  document.title = `${config.appName} - Try Now`;

  const handleStartSession = (): void => {
    setLoading(true);
    setError(undefined);
    startSession({
      appId: config.appId,
      serviceUrl: config.serviceUrl,
      appDefinition: config.appDefinition
    })
      .catch((err: Error) => {
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  return (
    <div className='App'>
      {loading ? <Loading /> : <LaunchApp appName={config.appName} onStartSession={handleStartSession} />}
      <ErrorComponent message={error} />
      <div className='App__footer'>
        <p>
          Powered by{' '}
          <a href='http://theia-cloud.io' target='_blank' rel='noreferrer'>
            Theia.Cloud
          </a>
        </p>
        <p>
          Having problems? Please{' '}
          <a target='_blank' href='https://github.com/eclipsesource/theia-cloud/issues' rel='noreferrer'>
            report an issue
          </a>
          .
        </p>
      </div>
    </div>
  );
}

const Loading = () => {
  return <div>
    <Spinner />
    <p className='Loading__description'>
      We will now spawn a dedicated Blueprint for you, hang in tight, this might take up to 3 minutes.
    </p>
  </div>
}

interface LaunchAppProps {
  appName: string;
  onStartSession: () => void;
}
const LaunchApp: React.FC<LaunchAppProps> = ({ appName, onStartSession }) => {
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  return (
    <div>
      <AppLogo />
      <p>
        <button
          className='App__try-now-button'
          disabled={!acceptedTerms}
          onClick={onStartSession} >
          Launch {appName} &nbsp;&nbsp;&rarr;
        </button>
      </p>
      <TermsAndConditions accepted={acceptedTerms} onTermsAccepted={setAcceptedTerms} />
    </div>
  );
}

interface ErrorComponentProps {
  message?: string;
}

const ErrorComponent: React.FC<ErrorComponentProps> = ({ message }) => {
  return message !== undefined ? (
    <div className='App__error-message'>
      <h2 >
        <strong>
          Oh no, something went wrong! &#9785;
        </strong>
      </h2>
      <pre>ERROR: {message}</pre>
    </div>
  ) : null
}
interface TermsAndConditionsProps {
  accepted: boolean;
  onTermsAccepted: (accepted: boolean) => void;
}
const openTerms = (): boolean => {
  window.open('/terms.html', 'popup', 'width=600,height=600');
  return false;
};
const TermsAndConditions: React.FC<TermsAndConditionsProps> = ({ accepted, onTermsAccepted }) => {
  return (
    <div className='terms-and-conditions'>
      <input
        id='accept-terms'
        type='checkbox'
        checked={accepted}
        onChange={ev => onTermsAccepted(ev.target.checked)}
      />
      <label htmlFor='accept-terms'>
        I accept the{' '}
        <a target='popup' href='/terms.html' onClick={openTerms}>
          terms and conditions.
        </a>
      </label>
    </div>
  );
}

export default App;
