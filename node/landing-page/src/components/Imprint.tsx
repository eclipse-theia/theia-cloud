import React from 'react';
import './Imprint.css';

interface ImprintProps {
  onNavigate?: (page: 'home' | 'imprint' | 'privacy') => void;
}

export const Imprint: React.FC<ImprintProps> = ({ onNavigate }) => {
  return (
    <div className='imprint'>
      <div className='imprint__container'>
        <div className='imprint__header'>
          <h1>Imprint</h1>
          <p>Legal information for TUM Theia Cloud</p>
          <button onClick={() => onNavigate ? onNavigate('home') : window.history.back()} className='imprint__back-btn'>← Back</button>
        </div>

        <div className='imprint__content'>
          <div className='imprint__card'>
            <h2>Publisher</h2>
            <div className='imprint__info'>
              <h3>Technical University of Munich</h3>
              <div className='imprint__details'>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Address:</span>
                  <span>Arcisstrasse 21, 80333 Munich</span>
                </div>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Phone:</span>
                  <span>+49-(0)89-289-01</span>
                </div>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Fax:</span>
                  <span>+49-(0)89-289-22000</span>
                </div>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Email:</span>
                  <span>poststelle(at)tum.de</span>
                </div>
              </div>
            </div>
          </div>

          <div className='imprint__card'>
            <h2>Authorized to represent</h2>
            <p>The Technical University of Munich is legally represented by the President Prof. Dr. Thomas F. Hofmann.</p>
          </div>

          <div className='imprint__card'>
            <h2>VAT identification number</h2>
            <p>DE811193231 (in accordance with § 27a of the German VAT tax act - UStG)</p>
          </div>

          <div className='imprint__card'>
            <h2>Responsible for content</h2>
            <div className='imprint__info'>
              <h3>Prof. Dr. Stephan Krusche</h3>
              <div className='imprint__details'>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Address:</span>
                  <span>Boltzmannstrasse 3, 85748 Garching</span>
                </div>
              </div>
            </div>
          </div>

          <div className='imprint__card'>
            <h2>Terms of use</h2>
            <p>Texts, images, graphics as well as the design of these Internet pages may be subject to copyright. The following are not protected by copyright according to §5 of copyright law (Urheberrechtsgesetz (UrhG)).</p>
            
            <p>Laws, ordinances, official decrees and announcements as well as decisions and officially written guidelines for decisions and other official works that have been published in the official interest for general knowledge, with the restriction that the provisions on prohibition of modification and indication of source in Section 62 (1) to (3) and Section 63 (1) and (2) UrhG apply accordingly.</p>
            
            <p>As a private individual, you may use copyrighted material for private and other personal use within the scope of Section 53 UrhG. Any duplication or use of objects such as images, diagrams, sounds or texts in other electronic or printed publications is not permitted without our agreement. This consent will be granted upon request by the person responsible for the content. The reprinting and evaluation of press releases and speeches are generally permitted with reference to the source. Furthermore, texts, images, graphics and other files may be subject in whole or in part to the copyright of third parties. The persons responsible for the content will also provide more detailed information on the existence of possible third-party rights.</p>
          </div>

          <div className='imprint__card'>
            <h2>Liability disclaimer</h2>
            <p>The information provided on this website has been collected and verified to the best of our knowledge and belief. However, there will be no warranty that the information provided is up-to-date, correct, complete, and available. There is no contractual relationship with users of this website.</p>
            
            <p>We accept no liability for any loss or damage caused by using this website. The exclusion of liability does not apply where the provisions of the German Civil Code (BGB) on liability in case of breach of official duty are applicable (§ 839 of the BGB). We accept no liability for any loss or damage caused by malware when accessing or downloading data or the installation or use of software from this website.</p>
            
            <p>Where necessary in individual cases: the exclusion of liability does not apply to information governed by the Directive 2006/123/EC of the European Parliament and of the Council. This information is guaranteed to be accurate and up to date.</p>
          </div>

          <div className='imprint__card'>
            <h2>Links</h2>
            <p>Our own content is to be distinguished from cross-references ("links") to websites of other providers. These links only provide access for using third-party content in accordance with § 8 of the German telemedia act (TMG). Prior to providing links to other websites, we review third-party content for potential civil or criminal liability. However, a continuous review of third-party content for changes is not possible, and therefore we cannot accept any responsibility. For illegal, incorrect, or incomplete content, including any damage arising from the use or non-use of third-party information, liability rests solely with the provider of the website.</p>
          </div>

          <div className='imprint__card'>
            <h2>Research Group</h2>
            <div className='imprint__info'>
              <h3>TUM, CIT, Research Group for Applied Education Technologies</h3>
              <div className='imprint__details'>
                <div className='imprint__detail'>
                  <span className='imprint__label'>Version:</span>
                  <span>v4.3.0</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
