interface FooterProps {
  selectedAppDefinition: string;
}

export const Footer = ({ selectedAppDefinition }: FooterProps): JSX.Element => (
  <div className='App__footer'>
    <p>
      <label htmlFor='selectapp'> {selectedAppDefinition} </label>
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
