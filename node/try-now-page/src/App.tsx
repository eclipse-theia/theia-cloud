import './App.css';

import { getTheiaCloudConfig, startWorkspace } from '@theia-cloud/common';
import { useState } from 'react';

import logo from './assets/logo.png';
import { Spinner } from './components/Spinner';

function App(): JSX.Element {
  const [config] = useState(() => getTheiaCloudConfig());
  const [error, setError] = useState<string>();
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [loading, setLoading] = useState(false);

  if (config === undefined) {
    return (
      <div className='App'>
        <strong>FATAL: Theia.Cloud configuration could not be found.</strong>
      </div>
    );
  }

  const handleStartWorkspace = (): void => {
    setLoading(true);
    setError(undefined);
    startWorkspace({
      appId: config.appId,
      workspaceServiceUrl: config.workspaceServiceUrl,
      workspaceTemplate: config.workspaceTemplate
    })
      .catch((err: Error) => {
        setError(err.message);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  const openTerms = (): boolean => {
    window.open('/terms.html', 'popup', 'width=600,height=600');
    return false;
  };

  return (
    <div className='App'>
      <img src={logo} className='App__logo' alt='logo' />
      <p>
        Powered by{' '}
        <a href='https://github.com/eclipsesource/theia-cloud' target='_blank' rel='noreferrer'>
          Theia.Cloud
        </a>
      </p>
      <p>
        <input
          id='accept-terms'
          type='checkbox'
          checked={acceptedTerms}
          onChange={ev => setAcceptedTerms(ev.target.checked)}
        />
        <label htmlFor='accept-terms'>
          I accept the{' '}
          <a target='popup' href='/terms.html' onClick={openTerms}>
            terms and conditions.
          </a>
        </label>
      </p>
      {loading ? (
        <>
          <Spinner />
          <p>We will now spawn a dedicated Blueprint for you, hang in tight, this might take up to 3 minutes.</p>
        </>
      ) : (
        <p>
          <button disabled={!acceptedTerms} onClick={handleStartWorkspace} className='App__try-now-button'>
            Launch Theia Blueprint
          </button>
        </p>
      )}
      {error !== undefined && (
        <p>
          <strong>ERROR: {error}</strong>
        </p>
      )}
      <p>
        Having problems? Please{' '}
        <a target='_blank' href='https://github.com/eclipsesource/theia-cloud/issues' rel='noreferrer'>
          report an issue
        </a>
        .
      </p>
    </div>
  );
}

export default App;
