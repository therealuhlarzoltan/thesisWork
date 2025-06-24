const errorMessages = {
    "email.blank": "Az email mező nem lehet üres.",
    "email.null": "Hiányzó email cím.",
    "password.blank": "A jelszó mező nem lehet üres.",
    "password.null": "Hiányzó jelszó.",
    "password.length": "A jelszó hoszzának 8 és 64 karakter között kell lennie.",
    "arrival.date.before.now": "Az érkezési idő nem lehet a múltban.",
    "changes.negative": "Az átszállások száma nem lehet negatív.",
    "arrival.date.before.departure": "Az érkezési időt nem előzheti meg az indulási.",
    "date.missing": "Hiányzó dátum.",
    "from.empty": "Az indulási állomás üres",
    "to.empty": "Az érkezési állomás üres"
};

export function translateError(key, fallback) {
    return errorMessages[key] ?? `${key}: ${fallback}`;
}