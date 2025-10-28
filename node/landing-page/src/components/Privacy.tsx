import React from 'react';
import './Privacy.css';

interface PrivacyProps {
  onNavigate?: (page: 'home' | 'imprint' | 'privacy') => void;
}

export const Privacy: React.FC<PrivacyProps> = ({ onNavigate }) => {
  return (
    <div className='privacy'>
      <div className='privacy__container'>
        <div className='privacy__header'>
          <h1>Privacy Policy</h1>
          <p>Data protection information for TUM Theia Cloud</p>
          <button onClick={() => onNavigate ? onNavigate('home') : window.history.back()} className='privacy__back-btn'>← Back</button>
        </div>

        <div className='privacy__content'>
          <div className='privacy__card'>
            <h2>Data Controller</h2>
            <p>The Technical University of Munich (TUM) is responsible for the processing of personal data in connection with the TUM Theia Cloud service.</p>
          </div>

          <div className='privacy__card'>
            <h2>Data Processing</h2>
            <p>We process personal data in accordance with the General Data Protection Regulation (GDPR) and the German Federal Data Protection Act (BDSG).</p>
            
            <div className='privacy__info'>
              <h3>Types of Data Processed:</h3>
              <ul className='privacy__list'>
                <li>User account information (email, username)</li>
                <li>Usage data for service provision</li>
                <li>Technical data (IP addresses, browser information)</li>
                <li>Session data for IDE functionality</li>
              </ul>
            </div>
          </div>

          <div className='privacy__card'>
            <h2>Purpose of Processing</h2>
            <p>Personal data is processed for the following purposes:</p>
            <ul className='privacy__list'>
              <li>Provision of the TUM Theia Cloud service</li>
              <li>User authentication and authorization</li>
              <li>Technical support and maintenance</li>
              <li>Compliance with legal obligations</li>
            </ul>
          </div>

          <div className='privacy__card'>
            <h2>Legal Basis</h2>
            <p>Data processing is based on:</p>
            <ul className='privacy__list'>
              <li>Art. 6(1)(a) GDPR (consent) for optional features</li>
              <li>Art. 6(1)(b) GDPR (contract performance) for service provision</li>
              <li>Art. 6(1)(f) GDPR (legitimate interests) for technical requirements</li>
            </ul>
          </div>

          <div className='privacy__card'>
            <h2>Data Retention</h2>
            <p>Personal data is retained only as long as necessary for the purposes outlined above or as required by law. User data is typically deleted after account termination or after a period of inactivity.</p>
          </div>

          <div className='privacy__card'>
            <h2>Your Rights</h2>
            <p>Under the GDPR, you have the right to:</p>
            <ul className='privacy__list'>
              <li>Access your personal data (Art. 15 GDPR)</li>
              <li>Rectify inaccurate data (Art. 16 GDPR)</li>
              <li>Erase your data (Art. 17 GDPR)</li>
              <li>Restrict processing (Art. 18 GDPR)</li>
              <li>Data portability (Art. 20 GDPR)</li>
              <li>Object to processing (Art. 21 GDPR)</li>
            </ul>
          </div>

          <div className='privacy__card'>
            <h2>Contact</h2>
            <p>For questions regarding data protection, please contact:</p>
            <div className='privacy__info'>
              <h3>Data Protection Officer</h3>
              <div className='privacy__details'>
                <div className='privacy__detail'>
                  <span className='privacy__label'>Institution:</span>
                  <span>Technical University of Munich</span>
                </div>
                <div className='privacy__detail'>
                  <span className='privacy__label'>Email:</span>
                  <span>datenschutz(at)tum.de</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
