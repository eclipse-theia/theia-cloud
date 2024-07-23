interface ErrorComponentProps {
  message?: string;
}

export const ErrorComponent: React.FC<ErrorComponentProps> = ({ message }: ErrorComponentProps) =>
  message !== undefined ? (
    <div className='App__error-message'>
      <h2>
        <strong>Oh no, something went wrong! &#9785;</strong>
      </h2>
      <pre>ERROR: {message}</pre>
    </div>
  ) : // eslint-disable-next-line no-null/no-null
    null;
