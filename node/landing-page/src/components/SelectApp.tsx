import { AppDefinition } from '../../../common/src/config.ts';

interface SelectAppProps {
  appDefinitions: AppDefinition[] | undefined;
  onStartSession: (appDefinition: string) => void;
}
export const SelectApp: React.FC<SelectAppProps> = ({
  appDefinitions,
  onStartSession
}: SelectAppProps) => (
  <div className='App__grid'>
    {appDefinitions && appDefinitions.map((app, index) => (
      <button
        key={index}
        className='App__grid-item'
        onClick={() => onStartSession(app.appId)}
      >
        {app.logo && (
          <img src={`data:image/png;base64,${app.logo}`} alt={`${app.appName} logo`} className='App__grid-item-logo' />
        )}
        Launch
        <br />
        {app.appName}
      </button>
    ))}
  </div>
);

