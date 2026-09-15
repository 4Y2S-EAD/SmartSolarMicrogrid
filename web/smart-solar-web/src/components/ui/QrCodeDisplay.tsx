import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

type QrCodeDisplayProps = {
  value: string;
  size?: number;
  className?: string;
};

export default function QrCodeDisplay({ value, size = 200, className = '' }: QrCodeDisplayProps) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!value) {
      setError('No token to encode');
      return;
    }
    QRCode.toDataURL(value, {
      width: size,
      margin: 2,
      color: { dark: '#0f172a', light: '#ffffff' },
      errorCorrectionLevel: 'M',
    })
      .then((url) => {
        setDataUrl(url);
        setError(null);
      })
      .catch((err) => setError(err.message));
  }, [value, size]);

  if (error) {
    return (
      <div
        className={`flex items-center justify-center bg-gray-50 rounded-xl text-sm text-gray-400 ${className}`}
        style={{ width: size, height: size }}
      >
        QR Error
      </div>
    );
  }

  if (!dataUrl) {
    return (
      <div
        className={`flex items-center justify-center bg-gray-50 rounded-xl animate-pulse ${className}`}
        style={{ width: size, height: size }}
      />
    );
  }

  return (
    <img
      src={dataUrl}
      alt="QR Code"
      className={`rounded-xl border border-gray-200 shadow-sm ${className}`}
      style={{ width: size, height: size }}
    />
  );
}
