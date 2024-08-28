import './App.css';

import { AppDefinition, getTheiaCloudConfig, LaunchRequest, PingRequest, TheiaCloud } from '@eclipse-theiacloud/common';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { useEffect, useState } from 'react';

import { AppLogo } from './components/AppLogo';
import { ErrorComponent } from './components/ErrorComponent';
import { Footer } from './components/Footer';
import { Header } from './components/Header';
import { Info } from './components/Info';
import { LaunchApp } from './components/LaunchApp';
import { Loading } from './components/Loading';
import { LoginButton } from './components/LoginButton';

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
        <strong>FATAL: Theia Cloud configuration could not be found.</strong>
      </div>
    );
  }

  if (!initialized) {
    initialAppName = config.appName;
    initialAppDefinition = config.appDefinition;
  }

  // ignore ESLint conditional rendering warnings.
  // If config === undefined, this is an unremediable situation anyway.
  /* eslint-disable react-hooks/rules-of-hooks */
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
        const pathBlueprintSelection = urlParams.get('appDef') || urlParams.get('appdef');
        if (
          // eslint-disable-next-line no-null/no-null
          pathBlueprintSelection !== null &&
          isDefaultSelectionValueValid(pathBlueprintSelection, config.appDefinition, config.additionalApps)
        ) {
          // eslint-disable-next-line no-null/no-null
          if (element !== null && config.additionalApps && config.additionalApps.length > 0) {
            (element as HTMLSelectElement).value = pathBlueprintSelection;
            setSelectedAppName(
              (element as HTMLSelectElement).options[(element as HTMLSelectElement).selectedIndex].text
            );
            setSelectedAppDefinition((element as HTMLSelectElement).value);
          } else {
            // If there are no additional apps, just use the application id as the name
            console.log('App definitition provided via URL parameter not found in additional apps');
            setSelectedAppDefinition(pathBlueprintSelection);
            setSelectedAppName(pathBlueprintSelection);
          }
        } else {
          setError('Invalid default selection value: ' + pathBlueprintSelection);
          console.error('Invalid default selection value: ' + pathBlueprintSelection);
        }
      }
      if (config.useKeycloak) {
        keycloakConfig = {
          url: config.keycloakAuthUrl,
          realm: config.keycloakRealm!,
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

      // Try to start the app if the app definition was changed via URL parameter
      if (selectedAppDefinition && selectedAppDefinition !== initialAppDefinition) {
        console.log('Starting session for ' + selectedAppDefinition);
        //handleStartSession(selectedAppDefinition);
      } else {
        console.log('Decided not to auto-start the session');
      }
    } else {
      console.log('App already initialized');
      console.log('Selected app definition: ' + selectedAppDefinition);
      console.log('Selected app name: ' + selectedAppName);
      console.log('Configured app definition: ' + config.appDefinition);
      console.log('Initial app definition: ' + initialAppDefinition);
      console.log('-----------------------------------');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
      console.log('App init changed');
      console.log('Selected app definition: ' + selectedAppDefinition);
      console.log('Selected app name: ' + selectedAppName);
      console.log('Configured app definition: ' + config.appDefinition);
      console.log('Initial app definition: ' + initialAppDefinition);
      console.log('-----------------------------------');
  }, [initialized]);

  /* eslint-enable react-hooks/rules-of-hooks */

  document.title = `${selectedAppName} - Theia`;

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
  const logoFileExtension = config.logoFileExtension ?? 'svg';

  return (
    <div className='App'>
      {config.useKeycloak ? (
        <Header email={email} authenticate={authenticate} logoutUrl={logoutUrl} />
      ) : (
        <div className='header'></div>
      )}
      <div className='body'>
        {loading ? (
          <Loading logoFileExtension={logoFileExtension} text={config.loadingText} />
        ) : (
          <div>
            <div>
              <AppLogo fileExtension={logoFileExtension} />
              <p>
                {needsLogin ? (
                  <LoginButton login={authenticate} />
                ) : (
                  <LaunchApp
                    appName={selectedAppName}
                    appDefinition={selectedAppDefinition}
                    onStartSession={handleStartSession}
                  />
                )}
              </p>
            </div>
          </div>
        )}
        <ErrorComponent message={error} />
        {!error && (
          <Info
            usesLogin={config.useKeycloak}
            disable={config.disableInfo}
            text={config.infoText}
            title={config.infoTitle}
          />
        )}
        <Footer selectedAppDefinition={selectedAppDefinition} />
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
