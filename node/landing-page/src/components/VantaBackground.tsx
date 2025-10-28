import { useEffect, useRef, useState } from 'react';
import { useTheme } from '../contexts/ThemeContext';

interface VantaBackgroundProps {
  children: React.ReactNode;
}

declare global {
  interface Window {
    THREE: any;
    VANTA: any;
  }
}

export const VantaBackground: React.FC<VantaBackgroundProps> = ({ children }) => {
  const vantaRef = useRef<HTMLDivElement>(null);
  const [vantaEffect, setVantaEffect] = useState<any>(null);
  const { theme } = useTheme();

  // Helper function to get CSS variable value and convert to hex
  const getCSSVariableAsHex = (variableName: string): number => {
    const value = getComputedStyle(document.documentElement)
      .getPropertyValue(variableName)
      .trim();
    
    // Convert #rrggbb to 0xrrggbb
    if (value.startsWith('#')) {
      return parseInt(value.slice(1), 16);
    }
    
    // Fallback to default values
    return 0x2a2a40;
  };

  const initializeVanta = () => {
    // Check if VANTA and THREE are loaded globally
    if (window.VANTA && window.THREE && vantaRef.current && !vantaEffect) {
      console.log('Initializing Vanta Birds effect...');
      
      try {
        // Get colors from CSS variables
        const backgroundColor = getCSSVariableAsHex('--vanta-bg');
        const color1 = getCSSVariableAsHex('--vanta-color1');
        const color2 = getCSSVariableAsHex('--vanta-color2');
        
        const effect = window.VANTA.BIRDS({
          el: vantaRef.current,
          mouseControls: false,
          touchControls: false,
          gyroControls: false,
          minHeight: 200.00,
          minWidth: 200.00,
          scale: 1.00,
          scaleMobile: 1.00,
          backgroundColor: backgroundColor,
          color1: color1,
          color2: color2,
          birdSize: 1.50,
          wingSpan: 10.00,
          speedLimit: 1.00,
          separation: 40.00,
          alignment: 35.00,
          cohesion: 45.00,
          quantity: 1.00
        });
        
        if (effect) {
          console.log('Vanta Birds effect initialized successfully');
          setVantaEffect(effect);
        }
      } catch (error) {
        console.error('Failed to initialize Vanta effect:', error);
        // Fallback: set background to match VantaJS backgroundColor
        if (vantaRef.current) {
          const fallbackBg = getComputedStyle(document.documentElement)
            .getPropertyValue('--vanta-bg')
            .trim() || '#2a2a40';
          vantaRef.current.style.background = fallbackBg;
        }
      }
    } else if (!window.VANTA || !window.THREE) {
      console.log('VANTA or THREE not loaded yet, retrying...');
      // Retry after a short delay
      setTimeout(initializeVanta, 500);
    }
  };

  useEffect(() => {
    // Add a small delay to ensure the scripts are loaded
    const timer = setTimeout(initializeVanta, 100);

    return () => {
      clearTimeout(timer);
      if (vantaEffect) {
        console.log('Destroying Vanta effect...');
        vantaEffect.destroy();
      }
    };
  }, [])

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      if (vantaEffect) {
        vantaEffect.resize();
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [vantaEffect]);

  // Reinitialize Vanta effect when theme changes
  useEffect(() => {
    if (vantaEffect) {
      // Destroy existing effect
      vantaEffect.destroy();
      setVantaEffect(null);
      
      // Reinitialize with new colors
      setTimeout(() => {
        initializeVanta();
      }, 100);
    }
  }, [theme]);

  return (
    <div style={{ position: 'relative', width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <div
        ref={vantaRef}
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          zIndex: -1,
          background: 'var(--vanta-bg, #2a2a40)', // Fallback background matching VantaJS backgroundColor
        }}
      />
      {children}
    </div>
  );
};
