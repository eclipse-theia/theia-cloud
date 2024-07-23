interface LaunchAppProps {
  appName: string;
  appDefinition: string;
  onStartSession: (appDefinition: string) => void;
}
export const LaunchApp: React.FC<LaunchAppProps> = ({
  appName,
  appDefinition,
  onStartSession
}: LaunchAppProps) => (
  <button
    className='App__try-now-button'
    onClick={() => onStartSession(appDefinition)}
  >
    Launch {appName} &nbsp;&nbsp;&rarr;
  </button>
);

