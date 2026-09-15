type StatusBadgeProps = {
  status: string;
};

const statusConfig: Record<string, { label: string; classes: string; dot: string }> = {
  active: { label: 'Active', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', dot: 'bg-emerald-500' },
  pending: { label: 'Pending', classes: 'bg-amber-50 text-amber-700 ring-amber-600/20', dot: 'bg-amber-500' },
  deactivated: { label: 'Deactivated', classes: 'bg-rose-50 text-rose-700 ring-rose-600/20', dot: 'bg-rose-500' },
  inactive: { label: 'Inactive', classes: 'bg-gray-100 text-gray-600 ring-gray-500/20', dot: 'bg-gray-400' },
  open: { label: 'Open', classes: 'bg-sky-50 text-sky-700 ring-sky-600/20', dot: 'bg-sky-500' },
  full: { label: 'Full', classes: 'bg-rose-50 text-rose-700 ring-rose-600/20', dot: 'bg-rose-500' },
  closed: { label: 'Closed', classes: 'bg-gray-100 text-gray-600 ring-gray-500/20', dot: 'bg-gray-400' },
  confirmed: { label: 'Confirmed', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', dot: 'bg-emerald-500' },
  cancelled: { label: 'Cancelled', classes: 'bg-rose-50 text-rose-700 ring-rose-600/20', dot: 'bg-rose-500' },
  completed: { label: 'Completed', classes: 'bg-teal-50 text-teal-700 ring-teal-600/20', dot: 'bg-teal-500' },
  no_show: { label: 'No Show', classes: 'bg-orange-50 text-orange-700 ring-orange-600/20', dot: 'bg-orange-500' },
  scanned: { label: 'Scanned', classes: 'bg-sky-50 text-sky-700 ring-sky-600/20', dot: 'bg-sky-500' },
  verified: { label: 'Verified', classes: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20', dot: 'bg-indigo-500' },
  finalized: { label: 'Finalized', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', dot: 'bg-emerald-500' },
  invalid: { label: 'Invalid', classes: 'bg-rose-50 text-rose-700 ring-rose-600/20', dot: 'bg-rose-500' },
  generated: { label: 'Generated', classes: 'bg-cyan-50 text-cyan-700 ring-cyan-600/20', dot: 'bg-cyan-500' },
  approved: { label: 'Approved', classes: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', dot: 'bg-emerald-500' },
  expired: { label: 'Expired', classes: 'bg-orange-50 text-orange-700 ring-orange-600/20', dot: 'bg-orange-500' },
};

export default function StatusBadge({ status }: StatusBadgeProps) {
  const config = statusConfig[status] ?? {
    label: status,
    classes: 'bg-gray-100 text-gray-600 ring-gray-500/20',
    dot: 'bg-gray-400',
  };

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${config.classes}`}>
      <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
}
