const errorMessages = {
    "email.blank": "Az email mező nem lehet üres.",
    "email.null": "Hiányzó email cím.",
    "password.blank": "A jelszó mező nem lehet üres.",
    "password.null": "Hiányzó jelszó.",
    "password.length": "A jelszó hoszzának 8 és 64 karakter között kell lennie."
};

export function translateError(key, fallback) {
    return errorMessages[key] ?? `${key}: ${fallback}`;
}