import { AppDefinition } from '../../../common/src/config.ts';

interface SelectAppProps {
  appDefinitions: AppDefinition[] | undefined;
  onStartSession: (appDefinition: string) => void;
}
export const SelectApp: React.FC<SelectAppProps> = ({
  appDefinitions,
  onStartSession
}: SelectAppProps) => {
  // Calculate the dynamic width based on the number of appDefinitions
  const dynamicWidth = appDefinitions
    ? Math.min(110 * appDefinitions.length, 450)
    : 100;
  
  return (
  <div className='App__grid' style={{ width: `${dynamicWidth}px`}}>
    {appDefinitions && appDefinitions.map((app, index) => (
      <button
        key={index}
        className='App__grid-item'
        style={{ marginBottom: '1rem' }}
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
  </div>);
};

