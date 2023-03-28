interface LaunchAppProps {
  acceptedTerms: boolean;
  appName: string;
  appDefinition: string;
  onStartSession: (appDefinition: string) => void;
}
export const LaunchApp: React.FC<LaunchAppProps> = ({
  acceptedTerms,
  appName,
  appDefinition,
  onStartSession
}: LaunchAppProps) => (
  <button
    className='App__try-now-button'
    disabled={!acceptedTerms}
    onClick={() => onStartSession(appDefinition)}
  >
    Launch {appName} &nbsp;&nbsp;&rarr;
  </button>
);

