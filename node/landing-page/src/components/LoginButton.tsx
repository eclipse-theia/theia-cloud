export interface LoginButtonProps {
  login: () => void;
}
export const LoginButton: React.FC<LoginButtonProps> = ({login}: LoginButtonProps) => (<button
  className='App__try-now-button'
  onClick={() => login()}
  data-testid="loginButton"
>
    Login
</button>);
