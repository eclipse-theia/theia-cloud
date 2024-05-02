import { Spinner } from './Spinner';

export const Loading = (): JSX.Element => (
  <div>
    <Spinner />
    <p className='Loading__description'>
      We will now spawn a dedicated Blueprint for you, hang in tight, this might take up to 3 minutes.
    </p>
  </div>
);
