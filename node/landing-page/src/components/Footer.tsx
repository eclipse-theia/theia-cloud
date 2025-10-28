import { FooterLinksConfig } from '@eclipse-theiacloud/common';

interface FooterProps {
  selectedAppDefinition: string;
  onNavigate?: (page: 'home' | 'imprint' | 'privacy') => void;
  footerLinks?: FooterLinksConfig;
}

export const Footer = ({ selectedAppDefinition, onNavigate, footerLinks }: FooterProps): JSX.Element => {
  const handleNavigation = (page: 'home' | 'imprint' | 'privacy') => {
    if (onNavigate) {
      onNavigate(page);
    } else {
      // Fallback to URL navigation
      window.location.href = page === 'home' ? '/' : `/${page}`;
    }
  };

  return (
  <div className='App__footer'>
    { selectedAppDefinition != '' && (
      <p>
        <label htmlFor='selectapp'> {selectedAppDefinition} </label>
      </p>)
     }
    
    <div className='App__footer__content'>
      <div className='App__footer__attribution'>
        {footerLinks?.attribution?.url ? (
          <a href={footerLinks.attribution.url} target='_blank' rel='noreferrer'>
            {footerLinks?.attribution?.text ? footerLinks.attribution.text : 'Built by TUM AET Team 👨‍💻'}
          </a>
        ) : (
          <a href='https://aet.cit.tum.de/' target='_blank' rel='noreferrer'>
            {footerLinks?.attribution?.text ? footerLinks.attribution.text : 'Built by TUM AET Team 👨‍💻'}
          </a>
        )}
        <span className='App__footer__separator'> · </span>
        <span>{footerLinks?.attribution?.version || 'v1.0.0'}</span>
      </div>

      <div className='App__footer__issues'>
        {footerLinks?.bugReport ? (
          <a
            target={footerLinks.bugReport.target || '_blank'}
            href={footerLinks.bugReport.url}
            rel={footerLinks.bugReport.rel || 'noreferrer'}
          >
            🐞 {footerLinks.bugReport.text}
          </a>
        ) : (
          <a target='_blank' href='https://github.com/eclipse-theia/theia-cloud/issues' rel='noreferrer'>🐞 Report a bug</a>
        )}
        {' or '}
        {footerLinks?.featureRequest ? (
          <a
            target={footerLinks.featureRequest.target || '_blank'}
            href={footerLinks.featureRequest.url}
            rel={footerLinks.featureRequest.rel || 'noreferrer'}
          >
            💡 {footerLinks.featureRequest.text}
          </a>
        ) : (
          <a target='_blank' href='https://github.com/eclipse-theia/theia-cloud/issues' rel='noreferrer'>💡 Request a feature</a>
        )}
      </div>

      <div className='App__footer__legal'>
        {footerLinks?.about ? (
          <a
            href={footerLinks.about.url}
            target={footerLinks.about.target || '_blank'}
            rel={footerLinks.about.rel || 'noreferrer'}
            className='App__footer__link'
          >
            {footerLinks.about.text}
          </a>
        ) : (
          <a href='https://ase-website-test.ase.cit.tum.de/' target='_blank' rel='noreferrer' className='App__footer__link'>About</a>
        )}
        <span className='App__footer__separator'>|</span>
        <button onClick={() => handleNavigation('imprint')} className='App__footer__link App__footer__button'>Imprint</button>
        <span className='App__footer__separator'>|</span>
        <button onClick={() => handleNavigation('privacy')} className='App__footer__link App__footer__button'>Privacy</button>
      </div>
    </div>
  </div>
  );
};
