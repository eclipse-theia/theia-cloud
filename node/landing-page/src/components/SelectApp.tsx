import { AppDefinition } from '../../../common/src/config.ts';

interface SelectAppProps {
  appDefinitions: AppDefinition[] | undefined;
  onStartSession: (appDefinition: string) => void;
}
export const SelectApp: React.FC<SelectAppProps> = ({
  appDefinitions,
  onStartSession
}: SelectAppProps) => {
  return (
    <div className='App__grid'>
      {appDefinitions && appDefinitions.map((app, index) => (
        <button
          key={index}
          className='App__grid-item'
          onClick={() => onStartSession(app.appId)}
          data-testid={`launch-app-${app.appId}`}
        >
          <img src={`/assets/logos/${app.appName.toLowerCase()}-logo.png`} alt={`${app.appName} logo`} className='App__grid-item-logo' />
          <div className='App__grid-item-launch'>Launch</div>
          <div className='App__grid-item-text'>{app.appName}</div>
        </button>
      ))}
    </div>
  );
};

