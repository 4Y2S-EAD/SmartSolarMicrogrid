import type { QrTransactionStatus } from '@/lib/api';

type TimelineStep = {
  label: string;
  status: QrTransactionStatus;
  time: string | null;
};

type TransactionTimelineProps = {
  currentStatus: QrTransactionStatus;
  steps: TimelineStep[];
};

export default function TransactionTimeline({ currentStatus, steps }: TransactionTimelineProps) {
  const statusOrder: QrTransactionStatus[] = [
    'generated', 'approved', 'scanned', 'verified', 'completed',
  ];

  const cancelledOrExpired = ['cancelled', 'expired', 'invalid'].includes(currentStatus);
  const currentIdx = statusOrder.indexOf(currentStatus);

  return (
    <div className="relative">
      {/* Vertical line */}
      <div className="absolute left-5 top-0 bottom-0 w-0.5 bg-gray-200" />

      <div className="space-y-6">
        {steps.map((step, i) => {
          const stepIdx = statusOrder.indexOf(step.status);
          const done = !cancelledOrExpired && currentIdx > stepIdx;
          const current = !cancelledOrExpired && currentIdx === stepIdx;
          const isCancelledStep = cancelledOrExpired && step.status === currentStatus;

          return (
            <div key={i} className="relative flex items-start gap-4">
              <div
                className={`relative z-10 flex h-10 w-10 items-center justify-center rounded-full ring-4 ring-white transition-all ${
                  done
                    ? 'bg-emerald-500 text-white'
                    : current
                    ? 'bg-amber-500 text-white animate-pulse'
                    : isCancelledStep
                    ? 'bg-rose-500 text-white'
                    : 'bg-gray-100 text-gray-400'
                }`}
              >
                {done && (
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                  </svg>
                )}
                {current && <span className="h-2.5 w-2.5 rounded-full bg-white" />}
                {isCancelledStep && (
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                )}
                {!done && !current && !isCancelledStep && (
                  <span className="h-2.5 w-2.5 rounded-full bg-gray-300" />
                )}
              </div>
              <div className="flex-1 pt-1.5">
                <div
                  className={`text-sm font-medium ${
                    done || current ? 'text-gray-900' : isCancelledStep ? 'text-rose-700' : 'text-gray-400'
                  }`}
                >
                  {step.label}
                </div>
                {step.time ? (
                  <div className="text-xs text-gray-400 mt-0.5">
                    {new Date(step.time).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                  </div>
                ) : (
                  !done && !current && !isCancelledStep && (
                    <div className="text-xs text-gray-300 mt-0.5">Pending</div>
                  )
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
