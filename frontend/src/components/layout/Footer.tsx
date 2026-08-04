import { APP_VERSION } from '@/constants/app';

export function Footer() {
  return (
    <footer className="border-t border-gray-200/70 px-4 py-5 text-center dark:border-gray-800">
      <p className="text-xs text-gray-500 dark:text-gray-400">
        © {new Date().getFullYear()} CloudNest — Your personal cloud, your files, anywhere.
      </p>
      <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">
        Frontend v{APP_VERSION} · Built with React, Vite &amp; Tailwind CSS
      </p>
    </footer>
  );
}
