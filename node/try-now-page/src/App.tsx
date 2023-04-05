/* eslint-disable max-len */
import './App.css';

import { AppDefinition, getTheiaCloudConfig, LaunchRequest, PingRequest, TheiaCloud } from '@eclipse-theiacloud/common';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { useEffect, useState } from 'react';

import { AppLogo } from './components/AppLogo';
import { ErrorComponent } from './components/ErrorComponent';
import { Footer } from './components/Footer';
import { Header } from './components/Header';
import { LaunchApp } from './components/LaunchApp';
import { Loading } from './components/Loading';
import { LoginButton } from './components/LoginButton';
import { LoginInfo } from './components/LoginInfo';
import { TermsAndConditions } from './components/TermsAndConditions';

// global state to be kept between render calls
let initialized = false;
let initialAppName = '';
let initialAppDefinition = '';
let keycloakConfig: KeycloakConfig | undefined = undefined;

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

  if (!initialized) {
    initialAppName = config.appName;
    initialAppDefinition = config.appDefinition;
  }

  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [selectedAppName, setSelectedAppName] = useState(initialAppName);
  const [selectedAppDefinition, setSelectedAppDefinition] = useState(initialAppDefinition);

  const [email, setEmail] = useState<string>();
  const [token, setToken] = useState<string>();
  const [logoutUrl, setLogoutUrl] = useState<string>();

  useEffect(() => {
    if (!initialized) {
      initialized = true;
      const element = document.getElementById('selectapp');
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has('appDef') || urlParams.has('appdef')) {
        const defaultSelection = urlParams.get('appDef') || urlParams.get('appdef');
        if (
          // eslint-disable-next-line no-null/no-null
          defaultSelection !== null &&
          isDefaultSelectionValueValid(defaultSelection, config.appDefinition, config.additionalApps)
        ) {
          // eslint-disable-next-line no-null/no-null
          if (element !== null && config.additionalApps && config.additionalApps.length > 0) {
            (element as HTMLSelectElement).value = defaultSelection;
            setSelectedAppName(
              (element as HTMLSelectElement).options[(element as HTMLSelectElement).selectedIndex].text
            );
            setSelectedAppDefinition((element as HTMLSelectElement).value);
          } else {
            // If there are no additional apps, just use the application id as the name
            setSelectedAppDefinition(defaultSelection);
            setSelectedAppName(defaultSelection);
          }
          initialAppName = selectedAppName;
          initialAppDefinition = selectedAppDefinition;
          console.log('Set ' + defaultSelection + ' as default selection');
        }
      }
      if (config.useKeycloak) {
        keycloakConfig = {
          url: config.keycloakAuthUrl,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          realm: config.keycloakRealm!,
          // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
          clientId: config.keycloakClientId!
        };
        const keycloak = Keycloak(keycloakConfig);
        keycloak
          .init({
            onLoad: 'check-sso',
            redirectUri: window.location.href,
            checkLoginIframe: false
          })
          .then(auth => {
            if (auth) {
              const parsedToken = keycloak.idTokenParsed;
              if (parsedToken) {
                const userMail = parsedToken.email;
                setToken(keycloak.idToken);
                setEmail(userMail);
                setLogoutUrl(keycloak.createLogoutUrl());
              }
            }
          })
          .catch(() => {
            console.error('Authentication Failed');
          });
      }
    }
  }, []);

  document.title = `${selectedAppName} - Try Now`;

  const authenticate = (): void => {
    const keycloak = Keycloak(keycloakConfig);
    keycloak
      .init({
        onLoad: 'login-required',
        redirectUri: window.location.href,
        checkLoginIframe: false
      })
      .then((auth: any) => {
        if (!auth) {
          window.location.reload();
        } else {
          const parsedToken = keycloak.idTokenParsed;
          if (parsedToken) {
            const userMail = parsedToken.email;
            setToken(keycloak.idToken);
            setEmail(userMail);
            setLogoutUrl(keycloak.createLogoutUrl());
          }
        }
      })
      .catch(() => {
        console.error('Authentication Failed');
        setError('Authentication failed');
      });
  };

  const handleStartSession = (appDefinition: string): void => {
    console.log('Launching ' + appDefinition);
    setLoading(true);
    setError(undefined);

    // first check if the service is available. if not we are doing maintenance and should adapt the error message accordingly
    TheiaCloud.ping(PingRequest.create(config.serviceUrl, config.appId))
      .then(() => {
        // ping successful continue with launch
        const workspace = config.useEphemeralStorage
          ? undefined
          : 'ws-' + config.appId + '-' + selectedAppDefinition + '-' + email;
        TheiaCloud.launchAndRedirect(
          config.useEphemeralStorage
            ? LaunchRequest.ephemeral(config.serviceUrl, config.appId, appDefinition, 5, email)
            : LaunchRequest.createWorkspace(config.serviceUrl, config.appId, appDefinition, 5, email, workspace),
          { timeout: 60000, retries: 5, accessToken: token }
        )
          .catch((err: Error) => {
            if (err && (err as any).status === 473) {
              setError(
                `The app definition '${appDefinition}' is not available in the cluster.\n` +
                  'Please try launching another application.'
              );
              return;
            }
            setError(err.message);
          })
          .finally(() => {
            setLoading(false);
          });
      })
      .catch((_err: Error) => {
        setError(
          'Sorry, we are performing some maintenance at the moment.\n' +
            "Please try again later. Usually maintenance won't last longer than 60 minutes.\n\n"
        );
        setLoading(false);
      });
  };

  const needsLogin = config.useKeycloak && !token;

  return (
    <div className='App'>
      {config.useKeycloak ? (
        <Header email={email} authenticate={authenticate} logoutUrl={logoutUrl} />
      ) : (
        <div className='header'></div>
      )}
      <div className='body'>
        {loading ? (
          <Loading />
        ) : (
          <div>
            <div>
              <AppLogo />
              <p>
                {
                  needsLogin
                    ? <LoginButton login={authenticate} />
                    : <LaunchApp
                      acceptedTerms={acceptedTerms}
                      appName={selectedAppName}
                      appDefinition={selectedAppDefinition}
                      onStartSession={handleStartSession}
                    />
                }
              </p>
              { !needsLogin && <TermsAndConditions accepted={acceptedTerms} onTermsAccepted={setAcceptedTerms} /> }
            </div>
          </div>
        )}
        <ErrorComponent message={error} />
        {
          needsLogin && <LoginInfo />
        }
        <Footer
          config={config}
          setSelectedAppName={setSelectedAppName}
          setSelectedAppDefinition={setSelectedAppDefinition}
        />
      </div>
    </div>
  );
}

function isDefaultSelectionValueValid(
  defaultSelection: string,
  appDefinition: string,
  additionalApps?: AppDefinition[]
): boolean {
  if (defaultSelection === appDefinition) {
    return true;
  }
  if (additionalApps && additionalApps.length > 0) {
    return additionalApps.map(def => def.appId).filter(appId => appId === defaultSelection).length > 0;
  }
  // If there are no additional apps explicitly configured, we accept any app definition given via url parameter
  return true;
}

export default App;
