import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

// https://vitejs.dev/config/
export default defineConfig(({command, mode}) => {
    const env = loadEnv(mode, process.cwd(), '');
    return {
        plugins: [react()],
        build: {
            outDir: mode === 'development' ? '../resources/static/' : './dist',
            emptyOutDir: true,
            rollupOptions: {
                output: {
                    manualChunks: {
                        mui_material: ['@mui/material'],
                        mui_icons: ['@mui/icons-material'],
                        mui_xdata: ['@mui/x-data-grid']
                    }
                }
            }
        },
    };
});
