window.theiaCloudConfig = {
  appId: 'asdfghjkl',
  appName: 'Theia Blueprint',
  // Set to false for demo/development mode without Keycloak server
  // Set to true when you have a real Keycloak instance running
  useKeycloak: false,
  serviceUrl: 'http://localhost:8081/service',
  appDefinition: 'ghcr.io/ls1intum/theia/java-17:latest',
  useEphemeralStorage: true,
  additionalApps: [
    {
      appId: "c-latest",
      appName: "C"
    },
    {
      appId: "java-17-latest", 
      appName: "Java"
    },
    {
      appId: "javascript-latest",
      appName: "Javascript"
    },
    {
      appId: "ocaml-latest",
      appName: "Ocaml"
    },
    {
      appId: "python-latest",
      appName: "Python"
    },
    {
      appId: "rust-latest",
      appName: "Rust"
    }
  ],
  disableInfo: true,
  infoText: '',
  infoTitle: '',
  loadingText: 'Preparing your personal Online IDE...',
  logoFileExtension: 'png',
  // Footer links configuration
  // All footer links are optional - if not provided, default values will be used
  footerLinks: {
    attribution: {
      text: 'Built by TUM AET Team 👨‍💻',
      url: 'https://aet.cit.tum.de/',
      version: 'v1.0.0'
    },
    bugReport: {
      text: 'Report a bug',
      url: 'https://github.com/eclipse-theia/theia-cloud/issues'
    },
    featureRequest: {
      text: 'Request a feature',
      url: 'https://github.com/eclipse-theia/theia-cloud/issues'
    },
    about: {
      text: 'About',
      url: 'https://ase-website-test.ase.cit.tum.de/'
    }
  },
  // Keycloak configuration - only used when useKeycloak: true
  // For development, you can use a local Keycloak or minikube setup
  // Example: "https://192.168.59.101.nip.io/keycloak" for minikube
  keycloakAuthUrl: "http://localhost:8080/auth/",
  keycloakRealm: "TheiaCloud",
  keycloakClientId: "theia-cloud",
};
