interface LoginInfoProps {
  showLoginInformation: boolean;
}

export const LoginInfo: React.FC<LoginInfoProps> = ({ showLoginInformation }: LoginInfoProps) =>
  showLoginInformation ? (
    <div className='App__info-message'>
      <h2>
        <strong>Please login to use this demo</strong>
      </h2>
      <pre>
        We do only need your login in data for technical reasons (e.g. avoiding crypto-mining or other harmful actions).
      </pre>
      <pre>We will not contact you via your e-mail or use it for any other marketing purposes.</pre>{' '}
      <pre>
        You can login using your Github account and do so on the top right of the page (You will be forwarded to out
        keycloak instance).
      </pre>
    </div>
  ) : // eslint-disable-next-line no-null/no-null
  null;
