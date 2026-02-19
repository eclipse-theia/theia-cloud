import './App.css';

import {
  AppDefinition,
  getTheiaCloudConfig,
  LaunchRequest,
  PingRequest,
  RequestOptions,
  TheiaCloud,
  TheiaCloudConfig
} from '@eclipse-theiacloud/common';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { useEffect, useState } from 'react';

import { AppLogo } from './components/AppLogo';
import { ErrorComponent } from './components/ErrorComponent';
import { Footer } from './components/Footer';
import { Header } from './components/Header';
import { Imprint } from './components/Imprint';
import { Info } from './components/Info';
import { LaunchApp } from './components/LaunchApp';
import { Loading } from './components/Loading';
import { LoginButton } from './components/LoginButton';
import { Privacy } from './components/Privacy';
import { SelectApp } from './components/SelectApp';
import { VantaBackground } from './components/VantaBackground';
import { initSentry } from './sentry';

// global state to be kept between render calls
let initialized = false;
let initialAppName = '';
let initialAppDefinition = '';
let keycloakConfig: KeycloakConfig | undefined = undefined;

function App(): JSX.Element {
  const [config] = useState(() => getTheiaCloudConfig());
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState<'home' | 'imprint' | 'privacy'>('home');

  useEffect(() => {
    if (config) {
      initSentry(config);
    }
  }, [config]);

  // Handle URL routing
  useEffect(() => {
    const updatePageFromUrl = () => {
      const path = window.location.pathname;
      if (path === '/imprint') {
        setCurrentPage('imprint');
      } else if (path === '/privacy') {
        setCurrentPage('privacy');
      } else {
        setCurrentPage('home');
      }
    };

    // Initial load
    updatePageFromUrl();

    // Listen for browser back/forward navigation
    window.addEventListener('popstate', updatePageFromUrl);

    return () => {
      window.removeEventListener('popstate', updatePageFromUrl);
    };
  }, []);

  // Navigation handler that updates both state and URL
  const handleNavigation = (page: 'home' | 'imprint' | 'privacy') => {
    const path = page === 'home' ? '/' : `/${page}`;

    // Update URL without page reload
    window.history.pushState({}, '', path);

    // Update state
    setCurrentPage(page);
  };

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
  const [user, setUser] = useState<string>();

  const [gitUri, setGitUri] = useState<string>();
  const [gitUser, setGitUser] = useState<string>();
  const [gitMail, setGitMail] = useState<string>();
  const [artemisToken, setArtemisToken] = useState<string>();
  const [artemisUrl, setArtemisUrl] = useState<string>();

  const [autoStart, setAutoStart] = useState<boolean>(false);

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
            appDef => appDef.serviceAuthToken === pathBlueprintSelection
          );
          setSelectedAppName(appDefinition ? appDefinition.appName : pathBlueprintSelection);
          setSelectedAppDefinition(appDefinition ? appDefinition.serviceAuthToken : pathBlueprintSelection);
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

    // Get artemisToken parameter from URL.
    if (urlParams.has('artemisToken')) {
      const artemisToken = urlParams.get('artemisToken');
      if (artemisToken) {
        setArtemisToken(artemisToken);
      }
    }

    // Get artemisUrl parameter from URL.
    if (urlParams.has('artemisUrl')) {
      const artemisUrl = urlParams.get('artemisUrl');
      if (artemisUrl) {
        setArtemisUrl(artemisUrl);
      }
    }

    // Get gitUser parameter from URL.
    if (urlParams.has('gitUser')) {
      const gitUser = urlParams.get('gitUser');
      if (gitUser) {
        setGitUser(gitUser);
      }
    }

    // Get gitMail parameter from URL.
    if (urlParams.has('gitMail')) {
      const gitMail = urlParams.get('gitMail');
      if (gitMail) {
        setGitMail(gitMail);
      }
    }

    // Get user parameter from URL (for anonymous mode when Keycloak is disabled).
    if (urlParams.has('user')) {
      const user = urlParams.get('user');
      if (user) {
        setUser(user);
      }
    }

    // Set default user for anonymous mode when Keycloak is disabled
    if (!config.useKeycloak && !urlParams.has('user')) {
      const randomId = Math.random().toString(36).substring(2, 10);
      setUser(`anonymous-${randomId}`);
    }

    if (config.useKeycloak) {
      keycloakConfig = {
        url: config.keycloakAuthUrl,
        realm: config.keycloakRealm!,
        clientId: config.keycloakClientId!
      };
      const keycloak = new Keycloak(keycloakConfig);

      keycloak
        .init({
          onLoad: 'check-sso',
          redirectUri: window.location.href,
          checkLoginIframe: false
        })
        .then(authenticated => {
          if (authenticated) {
            const parsedToken = keycloak.idTokenParsed;
            if (parsedToken) {
              const userMail = parsedToken.email;
              setToken(keycloak.idToken);
              setEmail(userMail);
              setUsername(parsedToken.preferred_username ?? userMail);
              setLogoutUrl(keycloak.createLogoutUrl());
              console.log(`Authenticated as ${parsedToken.preferred_username} (${userMail})`);
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
    if (!initialized) {
      console.log('Not initialized yet');
      return;
    }

    if (config.useKeycloak && !username) {
      console.log('No username set yet but required');
      return;
    }

    console.log('App init or username changed');
    console.log('Selected app definition: ' + selectedAppDefinition);
    console.log('Selected app name: ' + selectedAppName);
    console.log('Configured app definition: ' + config.appDefinition);
    console.log('Initial app definition: ' + initialAppDefinition);
    console.log('Git URI: ' + gitUri);
    console.log('Git User: ' + gitUser);
    console.log('Git Mail: ' + gitMail);
    console.log('Artemis Token: ' + artemisToken);
    console.log('Artemis URL: ' + artemisUrl);
    console.log('-----------------------------------');

    if (selectedAppDefinition && gitUri && artemisToken && !loading) {
      console.log('Checking auth, setting autoStart to true');
      // authenticate();
      setAutoStart(true);
      handleStartSession(selectedAppDefinition);
    } else {
      console.log('Setting autoStart to false');
      setAutoStart(false);
    }
  }, [initialized, username, user]);

  /* eslint-enable react-hooks/rules-of-hooks */

  document.title = 'TUM Theia Cloud';

  const authenticate: () => void = (): void => {
    const keycloak = new Keycloak(keycloakConfig);

    keycloak
      .init({
        redirectUri: window.location.origin + window.location.pathname,
        checkLoginIframe: false
      })
      .then((authenticated: boolean) => {
        if (!authenticated) {
          keycloak.login({
            redirectUri: window.location.origin + window.location.pathname,
            action: 'webauthn-register-passwordless:skip_if_exists'
          });
        } else {
          // If we are already authenticated (e.g. session existed but UI wasn't updated), update state
          const parsedToken = keycloak.idTokenParsed;
          if (parsedToken) {
            const userMail = parsedToken.email;
            setToken(keycloak.idToken);
            setEmail(userMail);
            setUsername(parsedToken.preferred_username ?? userMail);
            setLogoutUrl(keycloak.createLogoutUrl());
            console.log(`Authenticated as ${parsedToken.preferred_username} (${userMail})`);
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
    TheiaCloud.ping(PingRequest.create(config.serviceUrl, TheiaCloudConfig.getServiceAuthToken(config)))
      .then(() => {
        // ping successful continue with launch
        let workspace: string | undefined;

        if (config.useEphemeralStorage) {
          workspace = undefined;
          console.log('Launching ' + appDefinition + ' with ephemeral storage as not configured');
        } else {
          if (!gitUri) {
            workspace = undefined;
            console.log('Launching ' + appDefinition + ' with ephemeral storage as this is a Playground session');
          } else {
            // Artemis URLs look like: https://user@artemis.cit.tum.de/git/THEIATESTTESTEXERCISE/theiatesttestexercise-artemis_admin.git
            //                                                                                   ^^^^^^^^^^^^^^^^^^^^^ we need this part
            // First we split at the / character, get the last part, split at the - character and get the first part
            const repoName = gitUri?.split('/').pop()?.split('-')[0] ?? Math.random().toString().substring(2, 10);
            workspace = 'ws-' + appDefinition + '-' + repoName + '-' + (config.useKeycloak ? username : user);
            console.log('Launching ' + appDefinition + ' with persistent workspace ' + workspace);
          }
        }

        const requestOptions: RequestOptions = {
          timeout: 60000,
          retries: 5,
          accessToken: token
        };

        /*
        const sessionStartRequest: SessionStartRequest = {
          serviceUrl: config.serviceUrl,
          appId: config.appId,
          user: config.useKeycloak ? email! : user!,
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
          appId: TheiaCloudConfig.getServiceAuthToken(config),
          user: config.useKeycloak ? email! : user!,
          appDefinition: appDefinition,
          workspaceName: workspace,
          ephemeral: workspace === undefined, // No workspace = ephemeral session
          env: {
            fromMap: {
              THEIA: 'true',
              ARTEMIS_TOKEN: artemisToken!,
              ARTEMIS_URL: artemisUrl!,
              GIT_URI: gitUri!,
              GIT_USER: gitUser!,
              GIT_MAIL: gitMail!
            }
          }
        } satisfies LaunchRequest;

        // TheiaCloud.launchAndRedirect(
        // config.useEphemeralStorage
        //  ? LaunchRequest.ephemeral(config.serviceUrl, config.appId, appDefinition, 5, email)
        //  : LaunchRequest.createWorkspace(config.serviceUrl, config.appId, appDefinition, 5, email, workspace),

        // TheiaCloud.Session.list

        TheiaCloud.launchAndRedirect(launchRequest, requestOptions)
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

  // Render different pages based on currentPage state
  if (currentPage === 'imprint') {
    return (
      <div className='App'>
        <VantaBackground>
          <Imprint onNavigate={handleNavigation} />
        </VantaBackground>
      </div>
    );
  }

  if (currentPage === 'privacy') {
    return (
      <div className='App'>
        <VantaBackground>
          <Privacy onNavigate={handleNavigation} />
        </VantaBackground>
      </div>
    );
  }

  return (
    <div className='App'>
      <VantaBackground>
        <Header
          email={config.useKeycloak ? email : undefined}
          authenticate={config.useKeycloak ? authenticate : undefined}
          logoutUrl={config.useKeycloak ? logoutUrl : undefined}
        />
        <div className='body'>
          {loading ? (
            <Loading logoFileExtension={logoFileExtension} text={config.loadingText} />
          ) : (
            <div>
              <div>
                <div style={{ marginTop: '2rem' }}></div>
                <AppLogo fileExtension={logoFileExtension} />
                <h2 className="App__title">Choose your Online IDE</h2>
                <p>
                  {needsLogin ? (
                    <LoginButton login={authenticate} />
                  ) : autoStart ? (
                    <LaunchApp
                      appName={selectedAppName}
                      appDefinition={selectedAppDefinition}
                      onStartSession={handleStartSession}
                    />
                  ) : (
                    <SelectApp appDefinitions={config.additionalApps} onStartSession={handleStartSession} />
                  )}
                </p>
              </div>
            </div>
          )}
          <ErrorComponent message={error} />
          {!error && !loading && (
            <Info
              usesLogin={config.useKeycloak}
              disable={config.disableInfo}
              text={config.infoText}
              title={config.infoTitle}
            />
          )}
        </div>
        <Footer
          selectedAppDefinition={autoStart ? selectedAppDefinition : ''}
          onNavigate={handleNavigation}
          footerLinks={config.footerLinks}
        />
      </VantaBackground>
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
    return additionalApps.map(def => def.serviceAuthToken).filter(serviceAuthToken => serviceAuthToken === defaultSelection).length > 0;
  }
  // If there are no additional apps explicitly configured, we accept any app definition given via url parameter
  return true;
}

export default App;
