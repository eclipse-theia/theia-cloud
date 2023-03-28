export interface TermsAndConditionsProps {
  accepted: boolean;
  onTermsAccepted: (accepted: boolean) => void;
}
const openTerms = (): boolean => {
  window.open('./terms.html', 'popup', 'width=600,height=600');
  return false;
};
export const TermsAndConditions: React.FC<TermsAndConditionsProps> = ({
  accepted,
  onTermsAccepted
}: TermsAndConditionsProps) => (
  <div className='terms-and-conditions'>
    <input id='accept-terms' type='checkbox' checked={accepted} onChange={ev => onTermsAccepted(ev.target.checked)} />
    <label htmlFor='accept-terms'>
      I accept the{' '}
      <a target='popup' href='./terms.html' onClick={openTerms}>
        terms and conditions.
      </a>
    </label>
  </div>
);
