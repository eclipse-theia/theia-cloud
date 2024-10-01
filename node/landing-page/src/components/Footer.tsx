import { AppDefinition } from '@eclipse-theiacloud/common';

interface FooterProps {
  appDefinition: string,
  appName: string,
  additionalApps: AppDefinition[],
  setSelectedAppName: (value: React.SetStateAction<string>) => void,
  setSelectedAppDefinition: (value: React.SetStateAction<string>) => void
}

export const Footer = ({appDefinition, appName, additionalApps, setSelectedAppName, setSelectedAppDefinition}: FooterProps): JSX.Element => (
  <div className='App__footer'>
    {additionalApps.length > 0 && (
      <p>
        <label htmlFor='selectapp'>Select app to launch </label>
        <select
          name='apps'
          id='selectapp'
          onChange={event => {
            setSelectedAppName(event.target.options[event.target.selectedIndex].text);
            setSelectedAppDefinition(event.target.value);
          }}
        >
          <option value={appDefinition}>{appName}</option>
          {additionalApps.map((app: any, index: number) => (
            <option key={index + 1} value={app.appId}>
              {app.appName}
            </option>
          ))}
        </select>
      </p>
    )}
    <p>
      Powered by{' '}
      <a href='http://theia-cloud.io' target='_blank' rel='noreferrer'>
        Theia Cloud
      </a>
    </p>
    <p>
      Having problems? Please{' '}
      <a target='_blank' href='https://github.com/eclipse-theia/theia-cloud/issues' rel='noreferrer'>
        report an issue
      </a>
      .
    </p>
  </div>
);
