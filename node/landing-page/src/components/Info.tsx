interface InfoProps {
  /** Whether the Theia Cloud deployments requires authentication. */
  usesLogin: boolean;
  disable?: boolean;
  text?: string;
  title?: string;
}

export const Info: React.FC<InfoProps> = ({ usesLogin, disable, text, title }: InfoProps) => {
  if (disable) {
    // eslint-disable-next-line no-null/no-null
    return null;
  }
  const calculatedTitle = title ?? (usesLogin ? 'Login & launch Session' : 'Launch session');
  const calculatedText = text ?? (usesLogin ? DEFAULT_TEXT : DEFAULT_TEXT_NO_LOGIN);
  return (
    <div className='App__info-message'>
      <h2>
        <strong>{calculatedTitle}</strong>
      </h2>
      <p>{calculatedText}</p>
    </div>
  );
};

const DEFAULT_TEXT =
  'Use the button in the top right corner to login. ' +
  'Afterwards, launch a session by clicking the button above this notice. ' +
  'Once the session is ready, you will be redirected automatically.';
const DEFAULT_TEXT_NO_LOGIN =
  'Launch a session by clicking the button above this notice. Once the session is ready, you will be redirected automatically.';
