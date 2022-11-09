import './App.css';

import { AppDefinition, getTheiaCloudConfig, LaunchRequest, PingRequest, TheiaCloud } from '@eclipse-theiacloud/common';
import React, { useEffect, useState } from 'react';

import { AppLogo } from './components/AppLogo';
import { Spinner } from './components/Spinner';

// global state to be kept between render calls
let initialised = false;
let initialAppName = '';
let initialAppDefinition = '';

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

  if (!initialised) {
    initialAppName = config.appName;
    initialAppDefinition = config.appDefinition;
  }

  const [selectedAppName, setSelectedAppName] = useState(initialAppName);
  const [selectedAppDefinition, setSelectedAppDefinition] = useState(initialAppDefinition);

  useEffect(() => {
    if (!initialised) {
      initialised = true;
      const element = document.getElementById('selectapp');
      const urlParams = new URLSearchParams(window.location.search);
      // eslint-disable-next-line no-null/no-null
      if (element !== null && urlParams.has('appDef')) {
        const defaultSelection = urlParams.get('appDef');
        // eslint-disable-next-line no-null/no-null
        if (defaultSelection !== null && isDefaultSelectionValueValid(defaultSelection, config.appDefinition, config.additionalApps)) {
          (element as HTMLSelectElement).value = defaultSelection;
          setSelectedAppName((element as HTMLSelectElement).options[(element as HTMLSelectElement).selectedIndex].text);
          setSelectedAppDefinition((element as HTMLSelectElement).value);
          initialAppName = selectedAppName;
          initialAppDefinition = selectedAppDefinition;
          console.log('Set ' + defaultSelection + ' as default selection');
        }
      }
    }
  },[]);

  document.title = `${selectedAppName} - Try Now`;

  const handleStartSession = (appDefinition: string): void => {
    console.log('Launching ' + appDefinition);
    setLoading(true);
    setError(undefined);

    // first check if the service is available. if not we are doing maintenance and should adapt the error message accordingly
    TheiaCloud.ping(PingRequest.create(config.serviceUrl, config.appId))
      .then(() => {
        // ping successfull continue with launch
        TheiaCloud.launchAndRedirect(config.useEphemeralStorage
          ? LaunchRequest.ephemeral(config.serviceUrl, config.appId, appDefinition, 5)
          : LaunchRequest.createWorkspace(config.serviceUrl, config.appId, appDefinition, 5),
        { timeout: 60000, retries: 5 }
        )
          .catch((_err: Error) => {
            setError('Sorry, there are no more available instances in the cluster.\n'
          + 'Please try again later, instances are shut down after 30 minutes.\n\n'
          + 'Note: this is not a technical limit of Theia.cloud, but was intentionally set by us to keep this free offer within its intended budget.');
          })
          .finally(() => {
            setLoading(false);
          });
      })
      .catch((_err: Error) => {
        setError('Sorry, we are performing some maintenance at the moment.\n'
        + 'Please try again later. Usually maintenance won\'t last longer than 60 minutes.\n\n');
        setLoading(false);
      });
  };

  return (
    <div className='App'>
      {loading ? <Loading /> :
        <LaunchApp appName={selectedAppName} appDefinition={selectedAppDefinition} onStartSession={handleStartSession} />}
      <ErrorComponent message={error} />
      <div className='App__footer'>
        { config.additionalApps && config.additionalApps.length > 0 &&
          <p>
            <label htmlFor="selectapp">Select app to launch </label>
            <select name="apps" id="selectapp" onChange={event => {
              setSelectedAppName(event.target.options[event.target.selectedIndex].text);
              setSelectedAppDefinition(event.target.value);
            }}>
              <option value={config.appDefinition}>{config.appName}</option>
              { config.additionalApps.map((app, index) => <option key={index + 1} value={app.appId}>{app.appName}</option>)}
            </select>
          </p>
        }
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

const Loading = (): JSX.Element => <div>
  <Spinner />
  <p className='Loading__description'>
      We will now spawn a dedicated Blueprint for you, hang in tight, this might take up to 3 minutes.
  </p>
</div>;

interface LaunchAppProps {
  appName: string;
  appDefinition: string;
  onStartSession: (appDefinition: string) => void;
}
const LaunchApp: React.FC<LaunchAppProps> = ({ appName, appDefinition, onStartSession }) => {
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  return (
    <div>
      <AppLogo />
      <p>
        <button
          className='App__try-now-button'
          disabled={!acceptedTerms}
          onClick={() => onStartSession(appDefinition)} >
          Launch {appName} &nbsp;&nbsp;&rarr;
        </button>
      </p>
      <TermsAndConditions accepted={acceptedTerms} onTermsAccepted={setAcceptedTerms} />
    </div>
  );
};

interface ErrorComponentProps {
  message?: string;
}

const ErrorComponent: React.FC<ErrorComponentProps> = ({ message }) => message !== undefined ? (
  <div className='App__error-message'>
    <h2>
      <strong>
          Oh no, something went wrong! &#9785;
      </strong>
    </h2>
    <pre>ERROR: {message}</pre>
  </div>
// eslint-disable-next-line no-null/no-null
) : null;
interface TermsAndConditionsProps {
  accepted: boolean;
  onTermsAccepted: (accepted: boolean) => void;
}
const openTerms = (): boolean => {
  window.open('/terms.html', 'popup', 'width=600,height=600');
  return false;
};
const TermsAndConditions: React.FC<TermsAndConditionsProps> = ({ accepted, onTermsAccepted }) => (
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

function isDefaultSelectionValueValid(defaultSelection: string, appDefinition: string, additionalApps?: AppDefinition[]): boolean {
  if (defaultSelection === appDefinition) {
    return true;
  }
  if (additionalApps) {
    return additionalApps.map(def => def.appId).filter(appId => appId === defaultSelection).length > 0;
  }
  return false;
}

export default App;

