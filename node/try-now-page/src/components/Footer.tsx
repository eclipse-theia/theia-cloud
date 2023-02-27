export const Footer = (
  config: any,
  setSelectedAppName: (value: React.SetStateAction<string>) => void,
  setSelectedAppDefinition: (value: React.SetStateAction<string>) => void
): JSX.Element => (
  <div className='App__footer'>
    {config.additionalApps && config.additionalApps.length > 0 && (
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
          <option value={config.appDefinition}>{config.appName}</option>
          {config.additionalApps.map((app: any, index: number) => (
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
        Theia.Cloud
      </a>
    </p>
    <p>
      Having problems? Please{' '}
      <a target='_blank' href='https://github.com/eclipsesource/theia-cloud/issues' rel='noreferrer'>
        report an issue
      </a>
      .
    </p>
  </div>
);
