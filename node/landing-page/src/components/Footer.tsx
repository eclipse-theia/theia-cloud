import { AppDefinition } from '@eclipse-theiacloud/common';

interface FooterProps {
  selectedAppDefinition: AppDefinition;
}

export const Footer = ({ selectedAppDefinition }: FooterProps): JSX.Element => (
  <div className='App__footer'>
    <p>
      <label htmlFor='selectapp'> {selectedAppDefinition.appName} </label>
    </p>
    <p></p>
    <p>
      Having problems? Please{' '}
      <a target='_blank' href='https://github.com/eclipsesource/theia-cloud/issues' rel='noreferrer'>
        report an issue
      </a>
      .
    </p>
  </div>
);
