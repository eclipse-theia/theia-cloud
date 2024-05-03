import { Spinner } from './Spinner';

interface LoadingProps {
  logoFileExtension: string;
  text?: string;
}
export const Loading: React.FC<LoadingProps> = ({ logoFileExtension, text }: LoadingProps): JSX.Element => {
  const calculatedText =
    text ?? 'We are launching a dedicated session for you, hang in tight, this might take up to 3 minutes.';
  return (
    <div>
      <Spinner logoFileExtension={logoFileExtension}/>
      <p className='Loading__description'>{calculatedText}</p>
    </div>
  );
};
