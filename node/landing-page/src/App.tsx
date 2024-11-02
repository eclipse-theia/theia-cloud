import './App.css';

import { AppDefinition, getTheiaCloudConfig, PingRequest, RequestOptions, TheiaCloud } from '@eclipse-theiacloud/common';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { useEffect, useState } from 'react';

import { AppLogo } from './components/AppLogo';
import { ErrorComponent } from './components/ErrorComponent';
import { Footer } from './components/Footer';
import { Header } from './components/Header';
import { Info } from './components/Info';
import { LaunchApp } from './components/LaunchApp';
import { SelectApp } from './components/SelectApp';
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
  const [selectedAppName, setSelectedAppName] = useState<string>(initialAppName);
  const [selectedAppDefinition, setSelectedAppDefinition] = useState<string>(initialAppDefinition);

  const [email, setEmail] = useState<string>();
  const [username, setUsername] = useState<string>();
  const [token, setToken] = useState<string>();
  const [logoutUrl, setLogoutUrl] = useState<string>();

  const [gitUri, setGitUri] = useState<string>();
  const [gitToken, setGitToken] = useState<string>();
  const [artemisToken, setArtemisToken] = useState<string>();

  const [autoStart, setAutoStart] = useState<boolean>(true);

  if (!initialized) {
    const urlParams = new URLSearchParams(window.location.search);
    
    // Get appDef parameter from URL and set it as the default selection
    if (urlParams.has('appDef') || urlParams.has('appdef')) {
      const pathBlueprintSelection = urlParams.get('appDef') || urlParams.get('appdef');
      console.log('additionalApps: ' + JSON.stringify(config.additionalApps));
      if (
        // eslint-disable-next-line no-null/no-null
        pathBlueprintSelection !== null &&
        isDefaultSelectionValueValid(pathBlueprintSelection, config.appDefinition, config.additionalApps)
      ) {
        // eslint-disable-next-line no-null/no-null
        if (config.additionalApps && config.additionalApps.length > 0) {
          // Find the selected app definition in the additional apps
          const appDefinition = config.additionalApps.find(
            (appDef: AppDefinition) => appDef.appId === pathBlueprintSelection
          );
          setSelectedAppName(
            appDefinition ? appDefinition.appName : pathBlueprintSelection
          );
          setSelectedAppDefinition(appDefinition ? appDefinition.appId : pathBlueprintSelection);
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

    // Get gitUri parameter from URL.
    if (urlParams.has('gitUri')) {
      const gitUri = urlParams.get('gitUri');
      if (gitUri) {
        setGitUri(gitUri);
      }
    }

    // Get gitToken parameter from URL.
    if (urlParams.has('gitToken')) {
      const gitToken = urlParams.get('gitToken');
      if (gitToken) {
        setGitToken(gitToken);
      }
    }

    // Get artemisToken parameter from URL.
    if (urlParams.has('artemisToken')) {
      const artemisToken = urlParams.get('artemisToken');
      if (artemisToken) {
        setArtemisToken(artemisToken);
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
              setUsername(parsedToken.preferred_username ?? userMail);
              setLogoutUrl(keycloak.createLogoutUrl());
              console.log('Authenticated as ' + parsedToken.preferred_username + '(' + userMail + ')');
            }
          }
        })
        .catch(() => {
          console.error('Authentication Failed');
        });
    }
    initialized = true;
  }

  useEffect(() => {
    console.log('App init changed');
    console.log('Selected app definition: ' + selectedAppDefinition);
    console.log('Selected app name: ' + selectedAppName);
    console.log('Configured app definition: ' + config.appDefinition);
    console.log('Initial app definition: ' + initialAppDefinition);
    console.log('Git URI: ' + gitUri);
    console.log('Git Token: ' + gitToken);
    console.log('Artemis Token: ' + artemisToken);
    console.log('-----------------------------------');

    if (!initialized) {
      console.log('Not initialized yet');
      return;
    }

    if (selectedAppDefinition && gitUri && gitToken && !loading) {
      console.log('Checking auth, setting autoStart to true and starting session');
      //authenticate();
      setAutoStart(true);
      handleStartSession(selectedAppDefinition);
    } else {
      console.log('Setting autoStart to false');
      setAutoStart(false);
    }

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
            setUsername(parsedToken.preferred_username ?? userMail);
            setLogoutUrl(keycloak.createLogoutUrl());
            console.log('Authenticated as ' + parsedToken.preferred_username + '(' + userMail + ')');
          }
        }
      })
      .catch(() => {
        console.error('Authentication Failed');
        setError('Authentication failed');
      });
  };

  const handleStartSession = (appDefinition: string): void => {
    setLoading(true);
    setError(undefined);

    // first check if the service is available. if not we are doing maintenance and should adapt the error message accordingly
    TheiaCloud.ping(PingRequest.create(config.serviceUrl, config.appId))
      .then(() => {
        // ping successful continue with launch
        // Artemis URLs look like: https://user@artemis.cit.tum.de/git/THEIATESTTESTEXERCISE/theiatesttestexercise-artemis_admin.git
        //                                                                                   ^^^^^^^^^^^^^^^^^^^^^ we need this part
        // First we split at the / character, get the last part, split at the - character and get the first part
        const repoName = gitUri ? gitUri?.split('/').pop()?.split('-')[0] : Math.random().toString().substring(2, 10);

        const workspace = config.useEphemeralStorage
          ? undefined
          : 'ws-' + selectedAppDefinition + '-' + repoName + '-' + username;


        console.log('Launching ' + appDefinition + ' in workspace ' + workspace);

        const requestOptions: RequestOptions = {
          timeout: 60000,
          retries: 5,
          accessToken: token
        };
        
        /*
        const sessionStartRequest: SessionStartRequest = {
          serviceUrl: config.serviceUrl,
          appId: config.appId,
          user: email!,
          appDefinition,
          workspaceName: workspace,
          timeout: 180,
          env: {
            fromMap: {
              THEIA: 'true',
              ARTEMIS_TOKEN: artemisToken!,
              ARTEMIS_CLONE_URL: gitUri!
            }
          }
        };

        TheiaCloud.Session.startSession(
          sessionStartRequest,
          requestOptions
        ).catch((err: Error) => {
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
        */

          const launchRequest = {
            serviceUrl: config.serviceUrl,
            appId: config.appId,
            user: email!,
            appDefinition: appDefinition,
            workspaceName: workspace,
            env: {
              fromMap: {
                THEIA: 'true',
                ARTEMIS_TOKEN: artemisToken!,
                ARTEMIS_CLONE_URL: gitUri!
              }
            } 
          };

          //TheiaCloud.launchAndRedirect(
          //config.useEphemeralStorage
          //  ? LaunchRequest.ephemeral(config.serviceUrl, config.appId, appDefinition, 5, email)
          //  : LaunchRequest.createWorkspace(config.serviceUrl, config.appId, appDefinition, 5, email, workspace),
          
          TheiaCloud.launchAndRedirect(
            launchRequest,
            requestOptions
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
                  autoStart ? (
                    <LaunchApp
                      appName={selectedAppName}
                      appDefinition={selectedAppDefinition}
                      onStartSession={handleStartSession}
                    />
                  ) : (
                    <SelectApp
                      appDefinitions={config.additionalApps}
                      onStartSession={handleStartSession}
                    />
                  )
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
        <Footer selectedAppDefinition={autoStart ? selectedAppDefinition : ''} />
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
