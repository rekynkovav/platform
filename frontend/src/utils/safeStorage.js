const isDev = process.env.NODE_ENV === 'development';

export const safeStorage = {
    getItem: (key) => {
        try {
            const value = localStorage.getItem(key);
            if (value === null) return null;
            return JSON.parse(value);
        } catch (e) {
            console.error(`[safeStorage] Corrupted data for key "${key}", removing`);
            localStorage.removeItem(key);
            return null;
        }
    },
    setItem: (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (e) {
            console.error(`[safeStorage] Failed to set "${key}":`, e);
        }
    },
    removeItem: (key) => {
        localStorage.removeItem(key);
    },
    clearAll: () => {
        if (isDev) {
            localStorage.clear();
        }
    }
};
