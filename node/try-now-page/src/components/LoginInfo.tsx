export const LoginInfo: React.FC = () => (
  <div className='App__info-message'>
    <h2>
      <strong>Please login to use this demo</strong>
    </h2>
    <pre>
        We only need your login data for technical reasons (e.g. avoiding crypto-mining or other harmful actions).
    </pre>
    <pre>We will not contact you via your e-mail or use it for any other marketing purposes.</pre>{' '}
    <pre>
        You can login using your Github account (You will be forwarded to our Keycloak instance).
    </pre>
  </div>
);
